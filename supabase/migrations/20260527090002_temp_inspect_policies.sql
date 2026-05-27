-- Temporary: inspect RLS policies for debugging recursion
-- Will be reverted after inspection

CREATE OR REPLACE FUNCTION public.debug_list_policies()
RETURNS TABLE(
    schemaname text,
    tablename text,
    policyname text,
    permissive text,
    roles text[],
    cmd text,
    qual text,
    with_check text
) AS $$
    SELECT p.schemaname::text, p.tablename::text, p.policyname::text, p.permissive::text, p.roles::text[], p.cmd::text, p.qual::text, p.with_check::text
    FROM pg_policies p
    WHERE p.schemaname = 'public'
      AND p.tablename IN ('messages', 'conversations', 'conversation_participants')
    ORDER BY p.tablename, p.policyname;
$$ LANGUAGE sql SECURITY DEFINER STABLE;
