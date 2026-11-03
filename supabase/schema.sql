-- Run this entire script in Supabase Dashboard > SQL Editor.
create table if not exists public.guardian_state (
  id text primary key default 'main' check (id = 'main'),
  active boolean not null default false,
  last_seen timestamptz,
  battery integer check (battery between 0 and 100),
  latitude double precision,
  longitude double precision,
  accuracy double precision,
  location_at timestamptz,
  updated_at timestamptz not null default now()
);
insert into public.guardian_state (id) values ('main') on conflict (id) do nothing;

create table if not exists public.guardian_events (
  id bigint generated always as identity primary key,
  event_type text not null,
  detail text not null default '',
  created_at timestamptz not null default now()
);
create index if not exists guardian_events_created_at_idx on public.guardian_events (created_at desc);

-- The API uses the service-role key only on Vercel server-side.
-- Never put the service-role key in the Android app or public frontend.
alter table public.guardian_state enable row level security;
alter table public.guardian_events enable row level security;
revoke all on public.guardian_state from anon, authenticated;
revoke all on public.guardian_events from anon, authenticated;