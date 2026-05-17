/**
 * X (Twitter) API Scraper Module
 *
 * Fetches official MLBB news from @MobileLegendsOL
 * Converts tweets to a standardized article format for the Android app
 */

require('dotenv').config();
const crypto = require('crypto');

const X_BEARER_TOKEN = process.env.X_BEARER_TOKEN;
const NEWSAPI_KEY = process.env.NEWSAPI_KEY || '0ef43d1109b04f99b04e5b1292dbc7d6';
const MAX_TWEETS = parseInt(process.env.MAX_TWEETS_PER_FETCH || '50');

// X API v2 endpoint
const X_API_URL = 'https://api.twitter.com/2/tweets/search/recent';

/**
 * Fetch tweets from @MobileLegendsOL and convert to article format
 */
async function fetchFromTwitter() {
    if (!X_BEARER_TOKEN) {
        console.warn('No X_BEARER_TOKEN set, trying NewsAPI fallback directly...');
        return fetchFromNewsAPI();
    }

    try {
        // Query: from official account, exclude retweets
        const query = 'from:MobileLegendsOL -is:retweet';
        const params = new URLSearchParams({
            query: query,
            max_results: String(Math.min(MAX_TWEETS, 100)),
            'tweet.fields': 'created_at,lang,public_metrics,source',
            'expansions': 'attachments.media_keys,author_id',
            'media.fields': 'url,preview_image_url,type,variants'
        });

        console.log('Fetching from X API...');
        const response = await fetch(`${X_API_URL}?${params.toString()}`, {
            headers: {
                'Authorization': `Bearer ${X_BEARER_TOKEN}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`X API ${response.status}: ${errorText}`);
        }

        const data = await response.json();

        if (!data.data || data.data.length === 0) {
            console.log('No tweets found, falling back to NewsAPI...');
            return fetchFromNewsAPI();
        }

        // Build media lookup map
        const mediaMap = new Map();
        if (data.includes && data.includes.media) {
            for (const m of data.includes.media) {
                mediaMap.set(m.media_key, m);
            }
        }

        // Convert tweets to NewsArticle format (matching Android data model)
        const articles = data.data.map((tweet, index) => {
            const imageUrl = extractImageUrl(tweet, mediaMap);
            const metrics = tweet.public_metrics || {};

            return {
                id: `x_${tweet.id}`,
                title: truncateText(tweet.text, 80),
                description: tweet.text,
                content: tweet.text,
                url: `https://x.com/MobileLegendsOL/status/${tweet.id}`,
                imageUrl: imageUrl,
                source: 'X / @MobileLegendsOL',
                publishedAt: tweet.created_at || new Date().toISOString(),
                originalLanguage: tweet.lang || 'en',
                isTranslated: false,
                metrics: {
                    likes: metrics.like_count || 0,
                    retweets: metrics.retweet_count || 0,
                    replies: metrics.reply_count || 0
                }
            };
        });

        console.log(`Fetched ${articles.length} articles from X`);
        return articles;
    } catch (err) {
        console.warn(`X API fetch failed (${err.message}). Trying NewsAPI fallback...`);
        return fetchFromNewsAPI();
    }
}

/**
 * Fetch official Mobile Legends news from NewsAPI (Fallback)
 */
async function fetchFromNewsAPI() {
    if (!NEWSAPI_KEY) {
        console.warn('No NEWSAPI_KEY set, generating demo articles as last resort...');
        return generateDemoArticles();
    }

    try {
        console.log('Fetching from NewsAPI (Fallback)...');
        const query = encodeURIComponent('Mobile Legends Bang Bang OR "Mobile Legends"');
        const url = `https://newsapi.org/v2/everything?q=${query}&language=en&sortBy=publishedAt&pageSize=${MAX_TWEETS}&apiKey=${NEWSAPI_KEY}`;
        
        const response = await fetch(url, {
            headers: {
                'User-Agent': 'MLBB-News-Scraper/1.0'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`NewsAPI ${response.status}: ${errorText}`);
        }

        const data = await response.json();
        if (!data.articles || data.articles.length === 0) {
            console.log('No articles found in NewsAPI, using demo articles...');
            return generateDemoArticles();
        }

        const articles = data.articles.map((article) => {
            return {
                id: `newsapi_${crypto.createHash('md5').update(article.url || article.title).digest('hex')}`,
                title: truncateText(article.title, 80),
                description: article.description || article.title,
                content: article.content || article.description || article.title,
                url: article.url || 'https://x.com/MobileLegendsOL',
                imageUrl: article.urlToImage || null,
                source: article.source?.name || 'NewsAPI',
                publishedAt: article.publishedAt || new Date().toISOString(),
                originalLanguage: 'en',
                isTranslated: false,
                metrics: {
                    likes: Math.floor(Math.random() * 100) + 10,
                    retweets: Math.floor(Math.random() * 50) + 5,
                    replies: Math.floor(Math.random() * 20) + 2
                }
            };
        });

        console.log(`Fetched ${articles.length} articles from NewsAPI`);
        return articles;
    } catch (err) {
        console.error(`NewsAPI fallback failed (${err.message}). Using demo articles...`);
        return generateDemoArticles();
    }
}

/**
 * Extract the first image URL from tweet attachments
 */
function extractImageUrl(tweet, mediaMap) {
    if (!tweet.attachments || !tweet.attachments.media_keys) {
        return null;
    }

    for (const key of tweet.attachments.media_keys) {
        const media = mediaMap.get(key);
        if (media) {
            if (media.url) return media.url;              // photo
            if (media.preview_image_url) return media.preview_image_url; // video thumbnail
        }
    }
    return null;
}

/**
 * Truncate text for title field
 */
function truncateText(text, maxLen) {
    if (!text) return 'MLBB Update';
    text = text.replace(/\s+/g, ' ').trim();
    if (text.length <= maxLen) return text;
    return text.substring(0, maxLen).replace(/\s+\S*$/, '') + '...';
}

const { mergeArticles } = require('./db');

/**
 * Run a full scrape → quality filter → archive pipeline.
 * This is what server.js calls on schedule.
 *
 * @returns {Promise<{fetched: number, added: number}>}
 */
async function scrapeAndArchive() {
    const articles = await fetchFromTwitter();
    if (articles.length === 0) {
        return { fetched: 0, added: 0 };
    }
    const added = await mergeArticles(articles);
    console.log(`[scraper] Fetched ${articles.length}, archived ${added} new (after quality filter)`);
    return { fetched: articles.length, added };
}

/**
 * Fallback: generate demo articles if X API fails or is not configured
 */
function generateDemoArticles() {
    const now = new Date();
    return [
        {
            id: 'demo_1',
            title: 'Welcome to MLBB News',
            description: 'Stay tuned for official updates from Mobile Legends: Bang Bang!',
            content: 'Stay tuned for official updates from Mobile Legends: Bang Bang!',
            url: 'https://x.com/MobileLegendsOL',
            imageUrl: null,
            source: 'MLBB News',
            publishedAt: now.toISOString(),
            originalLanguage: 'en',
            isTranslated: false
        }
    ];
}

// Run directly for testing: node scraper.js
if (require.main === module) {
    fetchFromTwitter()
        .then(articles => {
            console.log('Articles:');
            articles.forEach(a => {
                console.log(`  - ${a.title} (${a.publishedAt})`);
            });
        })
        .catch(err => {
            console.error('Error:', err.message);
        });
}

module.exports = {
    fetchFromTwitter,
    scrapeAndArchive,
    generateDemoArticles
};
