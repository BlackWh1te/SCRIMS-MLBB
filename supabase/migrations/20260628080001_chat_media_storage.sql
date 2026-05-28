-- Create storage support for chat image messages.

INSERT INTO storage.buckets (id, name, public)
VALUES ('chat-media', 'chat-media', true)
ON CONFLICT (id) DO UPDATE SET public = EXCLUDED.public;

DROP POLICY IF EXISTS "Authenticated users can read chat media" ON storage.objects;
CREATE POLICY "Authenticated users can read chat media" ON storage.objects
    FOR SELECT USING (
        bucket_id = 'chat-media'
        AND auth.role() = 'authenticated'
    );

DROP POLICY IF EXISTS "Authenticated users can upload chat media" ON storage.objects;
CREATE POLICY "Authenticated users can upload chat media" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'chat-media'
        AND auth.role() = 'authenticated'
        AND owner = auth.uid()
    );

DROP POLICY IF EXISTS "Users can update own chat media" ON storage.objects;
CREATE POLICY "Users can update own chat media" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'chat-media'
        AND owner = auth.uid()
    )
    WITH CHECK (
        bucket_id = 'chat-media'
        AND owner = auth.uid()
    );

DROP POLICY IF EXISTS "Users can delete own chat media" ON storage.objects;
CREATE POLICY "Users can delete own chat media" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'chat-media'
        AND owner = auth.uid()
    );
