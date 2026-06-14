import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.0'

/**
 * Edge Function: delete-user
 *
 * Deletes a user from Supabase Auth using the service_role key.
 * The anon key CANNOT delete auth.users rows — this Edge Function
 * is required for the account deletion flow in the MLBB Scrim Host app.
 *
 * Security: Only callable by authenticated users. The function verifies
 * that the JWT user_id matches the user_id to delete (self-deletion only).
 */

Deno.serve(async (req) => {
  const supabaseUrl = Deno.env.get('SUPABASE_URL')!
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

  // Create admin client with service_role key
  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false }
  })

  // Get the user's JWT from the request
  const authHeader = req.headers.get('Authorization')
  if (!authHeader) {
    return new Response(JSON.stringify({ error: 'Missing Authorization header' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' }
    })
  }

  // Verify the requesting user
  const { data: { user }, error: authError } = await adminClient.auth.getUser(
    authHeader.replace('Bearer ', '')
  )

  if (authError || !user) {
    return new Response(JSON.stringify({ error: 'Invalid or expired token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' }
    })
  }

  const requestBody = await req.json().catch(() => ({}))
  const targetUserId = requestBody.user_id as string | undefined

  // Self-deletion only: target user must match JWT user
  if (!targetUserId || targetUserId !== user.id) {
    return new Response(JSON.stringify({ error: 'Can only delete your own account' }), {
      status: 403,
      headers: { 'Content-Type': 'application/json' }
    })
  }

  // Delete the auth user (this also cascades to profile via ON DELETE CASCADE)
  const { error: deleteError } = await adminClient.auth.admin.deleteUser(targetUserId)

  if (deleteError) {
    return new Response(JSON.stringify({ error: deleteError.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    })
  }

  return new Response(JSON.stringify({ success: true, deleted: targetUserId }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' }
  })
})
