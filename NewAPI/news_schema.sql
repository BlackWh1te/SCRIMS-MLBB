-- Run this in your Supabase SQL Editor to migrate the local News database to Supabase

CREATE TABLE public.news_articles (
    id text primary key,
    drip_index integer not null unique,
    title text not null,
    description text,
    content text,
    url text,
    image_url text,
    source text,
    published_at timestamp with time zone,
    archived_at timestamp with time zone default now(),
    original_language text default 'en',
    is_translated boolean default false,
    metrics jsonb default '{}'::jsonb
);

-- Enable Row Level Security (RLS) but allow anonymous read access (since it's public news)
ALTER TABLE public.news_articles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow anonymous read access to news"
ON public.news_articles
FOR SELECT
USING (true);
