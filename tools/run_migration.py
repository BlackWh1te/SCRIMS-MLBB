import psycopg2
import os

DB_HOST = os.environ.get("DB_HOST", "")
DB_NAME = os.environ.get("DB_NAME", "postgres")
DB_USER = os.environ.get("DB_USER", "postgres")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
DB_PORT = os.environ.get("DB_PORT", "5432")

if not DB_HOST or not DB_PASSWORD:
    print("ERROR: DB_HOST and DB_PASSWORD environment variables are required")
    exit(1)

filename = 'supabase_migration.sql'

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
    cursor.execute(sql_content)
    print(f"[OK] {filename} executed successfully")
    cursor.close()
    conn.close()
except Exception as e:
    print(f"[FAIL] Error executing {filename}: {e}")
