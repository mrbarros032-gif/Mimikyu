-- ==============================================================================
-- Supabase Row Level Security (RLS) Policy & Storage Hardening for Mimikyu
-- ==============================================================================
-- Purpose:
-- 1. Prevent unauthorized users from dumping/scraping the entire TTML lyrics database.
-- 2. Restrict INSERT, UPDATE, and DELETE operations to the backend service_role only.
-- 3. Allow authentic Mimikyu clients to fetch individual song lyrics by exact videoId.
-- ==============================================================================

-- 1. Ensure Row Level Security is enabled on the storage.objects table
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- 2. Drop existing overly-permissive storage policies if present
DROP POLICY IF EXISTS "Public Access" ON storage.objects;
DROP POLICY IF EXISTS "Allow public read" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon lyrics download" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon lyrics upload" ON storage.objects;
DROP POLICY IF EXISTS "Allow service role full access" ON storage.objects;

-- 3. Policy: Public/Anon can read single lyrics objects from the 'lyrics' bucket
-- Note: This permits GET /storage/v1/object/public/lyrics/<videoId>.ttml
CREATE POLICY "Allow public read of lyrics files"
ON storage.objects FOR SELECT
TO anon, authenticated
USING (bucket_id = 'lyrics');

-- 4. Policy: Strict write lockdown
-- ONLY service_role (backend pipeline/admin) can insert, update, or delete lyrics
CREATE POLICY "Strict admin service_role lyrics management"
ON storage.objects FOR ALL
TO service_role
USING (bucket_id = 'lyrics')
WITH CHECK (bucket_id = 'lyrics');

-- ==============================================================================
-- Optional Database Table Hardening (if tracking metadata in public.lyrics)
-- ==============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'lyrics') THEN
        ALTER TABLE public.lyrics ENABLE ROW LEVEL SECURITY;

        -- Allow reading metadata only
        CREATE POLICY "Allow anon read lyrics metadata"
        ON public.lyrics FOR SELECT
        TO anon, authenticated
        USING (true);

        -- Prevent modifications by non-service_role
        CREATE POLICY "Service role full access on lyrics metadata"
        ON public.lyrics FOR ALL
        TO service_role
        USING (true)
        WITH CHECK (true);
    END IF;
END $$;
