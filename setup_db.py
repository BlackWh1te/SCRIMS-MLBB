import psycopg2
import os

# Database connection — use environment variables
DB_HOST = os.environ.get("DB_HOST", "")
DB_NAME = os.environ.get("DB_NAME", "postgres")
DB_USER = os.environ.get("DB_USER", "postgres")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
DB_PORT = os.environ.get("DB_PORT", "5432")

if not DB_HOST or not DB_PASSWORD:
    print("ERROR: DB_HOST and DB_PASSWORD environment variables are required")
    exit(1)

def execute_sql_file(filename):
    """Read and execute SQL from a file"""
    print(f"\n{'='*50}")
    print(f"Executing {filename}...")
    print('='*50)

    with open(filename, 'r', encoding='utf-8') as f:
        sql_content = f.read()

    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD,
            port=DB_PORT,
            sslmode='require'
        )
        conn.autocommit = True
        cursor = conn.cursor()

        # Execute the entire SQL file at once
        cursor.execute(sql_content)
        print(f"[OK] {filename} executed successfully")

        cursor.close()
        conn.close()
        return True

    except Exception as e:
        print(f"[FAIL] Error executing {filename}: {e}")
        return False

def execute_sql_direct(sql):
    """Execute a single SQL statement"""
    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD,
            port=DB_PORT,
            sslmode='require'
        )
        conn.autocommit = True
        cursor = conn.cursor()
        cursor.execute(sql)
        cursor.close()
        conn.close()
        return True
    except Exception as e:
        print(f"Error: {e}")
        return False

def main():
    print("Starting database setup...")
    print(f"Connecting to {DB_HOST}...")

    # Test connection
    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD,
            port=DB_PORT,
            sslmode='require'
        )
        print("[OK] Database connection successful!")
        conn.close()
    except Exception as e:
        print(f"[FAIL] Database connection failed: {e}")
        return

    # Execute SQL files in order
    files = [
        'supabase/schema.sql',
        'supabase/rls_policies.sql',
        'supabase/triggers.sql',
        'supabase/functions.sql'
    ]

    for file in files:
        if os.path.exists(file):
            execute_sql_file(file)
        else:
            print(f"[FAIL] File not found: {file}")

    # Re-run RLS policies in case they failed due to existing policies
    print("\n" + "="*50)
    print("Re-running RLS policies (in case of conflicts)...")
    print("="*50)
    execute_sql_file('supabase/rls_policies.sql')

    # Set up storage buckets
    print("\n" + "="*50)
    print("Setting up storage buckets...")
    print("="*50)

    storage_sql = """
    INSERT INTO storage.buckets (id, name, public) VALUES
    ('match-screenshots', 'match-screenshots', false),
    ('user-avatars', 'user-avatars', true),
    ('team-logos', 'team-logos', true)
    ON CONFLICT (id) DO NOTHING;
    """

    if execute_sql_direct(storage_sql):
        print("[OK] Storage buckets created successfully")
    else:
        print("[FAIL] Failed to create storage buckets")

    # Set up storage policies
    print("\n" + "="*50)
    print("Setting up storage policies...")
    print("="*50)

    storage_policies = [
        """
        CREATE POLICY "Public read for avatars"
          ON storage.objects FOR SELECT
          USING (bucket_id = 'user-avatars');
        """,
        """
        CREATE POLICY "Users can upload own avatar"
          ON storage.objects FOR INSERT
          WITH CHECK (
            bucket_id = 'user-avatars'
            AND auth.uid()::text = (storage.foldername(name))[1]
          );
        """,
        """
        CREATE POLICY "Users can update own avatar"
          ON storage.objects FOR UPDATE
          USING (
            bucket_id = 'user-avatars'
            AND auth.uid()::text = (storage.foldername(name))[1]
          );
        """,
        """
        CREATE POLICY "Public read for team logos"
          ON storage.objects FOR SELECT
          USING (bucket_id = 'team-logos');
        """,
        """
        CREATE POLICY "Team leaders can upload logo"
          ON storage.objects FOR INSERT
          WITH CHECK (
            bucket_id = 'team-logos'
            AND EXISTS (
              SELECT 1 FROM teams
              WHERE id::text = (storage.foldername(name))[1]
              AND leader_id = auth.uid()
            )
          );
        """,
        """
        CREATE POLICY "Match participants can upload screenshots"
          ON storage.objects FOR INSERT
          WITH CHECK (
            bucket_id = 'match-screenshots'
            AND auth.uid() IS NOT NULL
          );
        """
    ]

    for policy in storage_policies:
        if execute_sql_direct(policy):
            print("[OK] Storage policy created")
        else:
            print("[INFO] Storage policy may already exist (skipping)")

    print("\n" + "="*50)
    print("Database setup complete!")
    print("="*50)
    print("\nNext steps:")
    print("1. Go to https://supabase.com/dashboard/project/efhbyrhxtsadbqjsfogc/auth/users")
    print("2. Click 'Add user' > 'Create new user'")
    print("3. Create your admin account")
    print("4. Get the user UUID and run:")
    print("   UPDATE profiles SET is_admin = true WHERE id = 'YOUR_USER_UUID';")

if __name__ == "__main__":
    main()
