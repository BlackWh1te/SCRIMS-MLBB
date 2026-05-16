/**
 * Supabase-backed Persistent Archive Database
 * Replaces the local 'articles-db.json' to make the server stateless.
 */

require('dotenv').config();

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;

const HEADERS = {
    'Content-Type': 'application/json',
    'apikey': SUPABASE_KEY || '',
    'Authorization': `Bearer ${SUPABASE_KEY || ''}`
};

// ── Quality Filter ────────────────────────────────────────────────

function isQualityArticle(article) {
    if (!article.description || article.description.trim().length < 15) return false;
    if (!article.title || article.title.trim().length < 10) return false;

    const spamWords = [
        'follow me', 'check bio', 'link in bio', 'dm me',
        'retweet and win', 'giveaway', 'free diamonds',
        'cheap mlbb diamonds', 'buy diamonds', 'sell account',
        'boosting service', 'elo boost', 'account for sale',
        'check pinned', 'promo code', 'use my code',
        'pls rt', 'please retweet', 'like and share',
        'i\'m live', 'streaming now', 'twitch.tv', 'youtube.com'
    ];
    const text = (article.title + ' ' + article.description).toLowerCase();
    if (spamWords.some(w => text.includes(w))) return false;

    if (article.metrics) {
        const totalEngagement = (article.metrics.likes || 0)
            + (article.metrics.retweets || 0)
            + (article.metrics.replies || 0);
        if (totalEngagement < 5) return false;
    }

    if (article.description.startsWith('@') && article.description.split(' ').length < 3) {
        return false;
    }
    return true;
}

// ── Database Operations ────────────────────────────────────────

async function getNextDripIndex() {
    try {
        const res = await fetch(`${SUPABASE_URL}/rest/v1/news_articles?select=drip_index&order=drip_index.desc&limit=1`, { headers: HEADERS });
        if (!res.ok) return 0;
        const data = await res.json();
        if (data && data.length > 0) return data[0].drip_index + 1;
    } catch (e) {
        console.error('Error fetching drip index', e);
    }
    return 0;
}

async function mergeArticles(newArticles) {
    let nextDripIndex = await getNextDripIndex();
    let added = 0;
    
    const resIds = await fetch(`${SUPABASE_URL}/rest/v1/news_articles?select=id`, { headers: HEADERS });
    let existingIds = new Set();
    if (resIds.ok) {
        const existing = await resIds.json();
        existingIds = new Set((existing || []).map(a => a.id));
    }

    for (const article of newArticles) {
        if (existingIds.has(article.id)) continue;
        if (!isQualityArticle(article)) continue;

        const dbArticle = {
            id: article.id,
            drip_index: nextDripIndex++,
            title: article.title,
            description: article.description,
            content: article.content,
            url: article.url,
            image_url: article.imageUrl,
            source: article.source,
            published_at: article.publishedAt,
            original_language: article.originalLanguage,
            is_translated: article.isTranslated,
            metrics: article.metrics
        };

        const postRes = await fetch(`${SUPABASE_URL}/rest/v1/news_articles`, {
            method: 'POST',
            headers: { ...HEADERS, 'Prefer': 'return=minimal' },
            body: JSON.stringify(dbArticle)
        });
        
        if (postRes.ok) added++;
    }
    return added;
}

function mapArticle(a) {
    return {
        id: a.id,
        dripIndex: a.drip_index,
        title: a.title,
        description: a.description,
        content: a.content,
        url: a.url,
        imageUrl: a.image_url,
        source: a.source,
        publishedAt: a.published_at,
        originalLanguage: a.original_language,
        isTranslated: a.is_translated,
        metrics: a.metrics
    };
}

async function getArticlesForUser(userOffset, limit = null) {
    let url = `${SUPABASE_URL}/rest/v1/news_articles?drip_index=lte.${userOffset}&order=drip_index.desc`;
    if (limit) url += `&limit=${limit}`;
    const res = await fetch(url, { headers: HEADERS });
    if (!res.ok) return [];
    const data = await res.json();
    return (data || []).map(mapArticle);
}

async function getTotalCount() {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/news_articles?select=id`, {
        method: 'HEAD',
        headers: { ...HEADERS, 'Prefer': 'count=exact' }
    });
    const count = res.headers.get('content-range');
    if (count) return parseInt(count.split('/')[1], 10);
    return 0;
}

async function getUnseenCount(userOffset) {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/news_articles?drip_index=gt.${userOffset}&select=id`, {
        method: 'HEAD',
        headers: { ...HEADERS, 'Prefer': 'count=exact' }
    });
    const count = res.headers.get('content-range');
    if (count) return parseInt(count.split('/')[1], 10);
    return 0;
}

async function getAllArticles() {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/news_articles?order=drip_index.desc`, { headers: HEADERS });
    if (!res.ok) return [];
    const data = await res.json();
    return (data || []).map(mapArticle);
}

async function resetDb() {
    await fetch(`${SUPABASE_URL}/rest/v1/news_articles?id=not.is.null`, {
        method: 'DELETE',
        headers: HEADERS
    });
}

module.exports = {
    mergeArticles,
    getArticlesForUser,
    getTotalCount,
    getUnseenCount,
    getAllArticles,
    resetDb,
    isQualityArticle
};
