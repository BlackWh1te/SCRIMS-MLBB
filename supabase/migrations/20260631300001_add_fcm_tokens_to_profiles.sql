-- Add FCM token column to profiles to support background push notifications via Firebase
ALTER TABLE profiles
ADD COLUMN IF NOT EXISTS fcm_token TEXT;
