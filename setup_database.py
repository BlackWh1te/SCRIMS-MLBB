import requests
import json

# Supabase configuration
SUPABASE_URL = "https://efhbyrhxtsadbqjsfogc.supabase.co"
SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVmaGJ5cmh4dHNhZGJxanNmb2djIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3ODYxOTMyNCwiZXhwIjoyMDk0MTk1MzI0fQ.PO8zk6lfZjSMdz3QzjJaJm8xLuRK-utQbQz3dHqAl_Q"

def execute_rpc_function(function_name, params):
    """Execute a Supabase RPC function"""
    url = f"{SUPABASE_URL}/rest/v1/rpc/{function_name}"
    headers = {
        "apikey": SERVICE_ROLE_KEY,
        "Authorization": f"Bearer {SERVICE_ROLE_KEY}",
        "Content-Type": "application/json"
    }

    response = requests.post(url, headers=headers, json=params)
    return response

# First, create a function that can execute dynamic SQL
create_exec_function = """
CREATE OR REPLACE FUNCTION exec_sql(sql_query TEXT)
RETURNS VOID AS $$
BEGIN
    EXECUTE sql_query;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
"""

# Try to execute this via the Supabase Management API
# Since we can't directly execute SQL, we need to use a different approach

print("To set up your database, please follow these manual steps:")
print("\n1. Go to: https://supabase.com/dashboard/project/efhbyrhxtsadbqjsfogc/sql")
print("2. Click 'New Query'")
print("3. Copy and paste the content of each SQL file in this order:")
print("   - supabase/schema.sql")
print("   - supabase/rls_policies.sql")
print("   - supabase/triggers.sql")
print("   - supabase/functions.sql")
print("4. After running all files, run this SQL for storage buckets:")
print("""
INSERT INTO storage.buckets (id, name, public) VALUES
('match-screenshots', 'match-screenshots', false),
('user-avatars', 'user-avatars', true),
('team-logos', 'team-logos', true)
ON CONFLICT (id) DO NOTHING;
""")
print("\n5. Create an admin user:")
print("   - Go to Authentication > Users")
print("   - Click 'Add user' > 'Create new user'")
print("   - After creation, get the user UUID and run:")
print("   UPDATE profiles SET is_admin = true WHERE id = 'YOUR_USER_UUID';")
