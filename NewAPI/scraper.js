/**
 * X (Twitter) API Scraper Module
 *
 * Fetches official MLBB news from @MobileLegendsOL
 * Converts tweets to a standardized article format for the Android app
 */

require('dotenv').config();

const X_BEARER_TOKEN = process.env.X_BEARER_TOKEN;
const MAX_TWEETS = parseInt(process.env.MAX_TWEETS_PER_FETCH || '50');

// X API v2 endpoint
const X_API_URL = 'https://api.twitter.com/2/tweets/search/recent';

/**
 * Fetch tweets from @MobileLegendsOL and convert to article format
 */
async function fetchFromTwitter() {
    if (!X_BEARER_TOKEN) {
        console.warn('No X_BEARER_TOKEN set, skipping Twitter fetch');
        return [];
    }

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
        console.log('No tweets found');
        return [];
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
            // Extra metadata for admin/debug
            metrics: {
                likes: metrics.like_count || 0,
                retweets: metrics.retweet_count || 0,
                replies: metrics.reply_count || 0
            }
        };
    });

    console.log(`Fetched ${articles.length} articles from X`);
    return articles;
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
