-- Execute only in the new SportPro Supabase project.
-- Intentionally fails if "imagenes" already exists: never expose a pre-existing private bucket.
begin;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('imagenes', 'imagenes', true, 2097152, array['image/png']);

-- Public download URLs are intentional for academy logos only.
-- The signed Firebase token must come from our project; sub is text, not a UUID.
create policy "sportpro_crest_insert"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'imagenes'
    and auth.jwt()->>'iss' = 'https://securetoken.google.com/sportpro-dpa-2026-rh'
    and auth.jwt()->>'aud' = 'sportpro-dpa-2026-rh'
    and auth.jwt()->>'app_role' = 'ADM'
    and (storage.foldername(name))[1] = auth.jwt()->>'sub'
    and array_length(storage.foldername(name), 1) = 1
    and storage.extension(name) = 'png'
);

-- Guard against permissive policies accidentally added later.
-- Public file download bypasses object SELECT policies by design.
create policy "sportpro_crest_guard"
on storage.objects as restrictive for all to public
using (
    bucket_id <> 'imagenes' or (
        auth.jwt()->>'iss' = 'https://securetoken.google.com/sportpro-dpa-2026-rh'
        and auth.jwt()->>'aud' = 'sportpro-dpa-2026-rh'
        and auth.jwt()->>'app_role' = 'ADM'
        and (storage.foldername(name))[1] = auth.jwt()->>'sub'
        and array_length(storage.foldername(name), 1) = 1
        and storage.extension(name) = 'png'
    )
)
with check (
    bucket_id <> 'imagenes' or (
        auth.jwt()->>'iss' = 'https://securetoken.google.com/sportpro-dpa-2026-rh'
        and auth.jwt()->>'aud' = 'sportpro-dpa-2026-rh'
        and auth.jwt()->>'app_role' = 'ADM'
        and (storage.foldername(name))[1] = auth.jwt()->>'sub'
        and array_length(storage.foldername(name), 1) = 1
        and storage.extension(name) = 'png'
    )
);
commit;
