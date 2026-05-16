import psycopg2

DB_HOST = "db.efhbyrhxtsadbqjsfogc.supabase.co"
DB_NAME = "postgres"
DB_USER = "postgres"
DB_PASSWORD = "+KjkpPVMr639E/n"
DB_PORT = "5432"

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
