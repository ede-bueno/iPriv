-- ====================================================================
-- PRIV - SUPABASE MIGRATION V1: INDIVIDUAL PRIVATE SPACE ARCHITECTURE
-- ====================================================================

-- 1. PROFILES
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL UNIQUE,
    full_name TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. SPACES
CREATE TABLE IF NOT EXISTS public.spaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    type TEXT NOT NULL DEFAULT 'PERSONAL',
    owner_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. SPACE MEMBERS
CREATE TABLE IF NOT EXISTS public.space_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES public.spaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'OWNER',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_space_user UNIQUE (space_id, user_id)
);

-- 4. PEOPLE
CREATE TABLE IF NOT EXISTS public.people (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES public.spaces(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    nickname TEXT,
    relationship TEXT,
    color_hex TEXT DEFAULT '#8E3BEE',
    bio_note TEXT,
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 5. GROUPS
CREATE TABLE IF NOT EXISTS public.groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES public.spaces(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    icon_name TEXT DEFAULT 'Group',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. MOMENTS
CREATE TABLE IF NOT EXISTS public.moments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES public.spaces(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    date TEXT,
    category TEXT DEFAULT 'Geral',
    color_hex TEXT DEFAULT '#00F5D4',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. COLLECTIONS
CREATE TABLE IF NOT EXISTS public.collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES public.spaces(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    icon_name TEXT DEFAULT 'Folder',
    color_hex TEXT DEFAULT '#FF4B72',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. TAGS
CREATE TABLE IF NOT EXISTS public.tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES public.spaces(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    CONSTRAINT unique_space_tag_name UNIQUE (space_id, name)
);

-- 9. MEMORIES
CREATE TABLE IF NOT EXISTS public.memories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES public.spaces(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    note TEXT,
    timestamp BIGINT NOT NULL,
    source TEXT NOT NULL DEFAULT 'WhatsApp',
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    in_trash BOOLEAN NOT NULL DEFAULT FALSE,
    trashed_at BIGINT,
    primary_person_id UUID REFERENCES public.people(id) ON DELETE SET NULL,
    moment_id UUID REFERENCES public.moments(id) ON DELETE SET NULL,
    collection_id UUID REFERENCES public.collections(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- 10. MEMORY ATTACHMENTS
CREATE TABLE IF NOT EXISTS public.memory_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    memory_id UUID NOT NULL REFERENCES public.memories(id) ON DELETE CASCADE,
    type TEXT NOT NULL DEFAULT 'AUDIO',
    remote_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type TEXT NOT NULL DEFAULT 'audio/ogg',
    checksum TEXT NOT NULL,
    duration_ms BIGINT DEFAULT 0,
    width INT DEFAULT 0,
    height INT DEFAULT 0,
    waveform_data TEXT,
    transcription TEXT,
    summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RELATIONSHIP TABLES
CREATE TABLE IF NOT EXISTS public.memory_people (
    memory_id UUID NOT NULL REFERENCES public.memories(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES public.people(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, person_id)
);

CREATE TABLE IF NOT EXISTS public.memory_moments (
    memory_id UUID NOT NULL REFERENCES public.memories(id) ON DELETE CASCADE,
    moment_id UUID NOT NULL REFERENCES public.moments(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, moment_id)
);

CREATE TABLE IF NOT EXISTS public.memory_collections (
    memory_id UUID NOT NULL REFERENCES public.memories(id) ON DELETE CASCADE,
    collection_id UUID NOT NULL REFERENCES public.collections(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, collection_id)
);

CREATE TABLE IF NOT EXISTS public.memory_tags (
    memory_id UUID NOT NULL REFERENCES public.memories(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES public.tags(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, tag_id)
);

-- AUTOMATIC PROFILE & PERSONAL SPACE INITIALIZATION TRIGGER
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    new_space_id UUID;
BEGIN
    -- 1. Insert Profile
    INSERT INTO public.profiles (id, email, full_name)
    VALUES (NEW.id, NEW.email, COALESCE(NEW.raw_user_meta_data->>'full_name', 'Usuário Priv'))
    ON CONFLICT (id) DO NOTHING;

    -- 2. Create Personal Space "Meu Priv"
    INSERT INTO public.spaces (name, description, type, owner_user_id)
    VALUES ('Meu Priv', 'Espaço pessoal de memórias', 'PERSONAL', NEW.id)
    RETURNING id INTO new_space_id;

    -- 3. Link Owner as ACTIVE Space Member
    INSERT INTO public.space_members (space_id, user_id, role, status)
    VALUES (new_space_id, NEW.id, 'OWNER', 'ACTIVE')
    ON CONFLICT (space_id, user_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.spaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.space_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.memories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.memory_attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.people ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.moments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.collections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tags ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Profiles strict isolation" ON public.profiles FOR ALL USING (auth.uid() = id);

CREATE POLICY "Spaces isolation" ON public.spaces FOR ALL USING (
    id IN (SELECT space_id FROM public.space_members WHERE user_id = auth.uid() AND status = 'ACTIVE')
);

CREATE POLICY "Space members isolation" ON public.space_members FOR ALL USING (
    space_id IN (SELECT space_id FROM public.space_members WHERE user_id = auth.uid() AND status = 'ACTIVE')
);

CREATE POLICY "Memories isolation" ON public.memories FOR ALL USING (
    space_id IN (SELECT space_id FROM public.space_members WHERE user_id = auth.uid() AND status = 'ACTIVE')
);

CREATE POLICY "Memory attachments isolation" ON public.memory_attachments FOR ALL USING (
    memory_id IN (
        SELECT id FROM public.memories WHERE space_id IN (
            SELECT space_id FROM public.space_members WHERE user_id = auth.uid() AND status = 'ACTIVE'
        )
    )
);

-- STORAGE SECURITY POLICY
-- Bucket: memories (Private)
-- Path: spaces/{spaceId}/memories/{memoryId}/{attachmentId}.{ext}
CREATE POLICY "Storage personal space path isolation" ON storage.objects FOR ALL USING (
    bucket_id = 'memories' AND
    (storage.foldername(name))[2]::uuid IN (
        SELECT space_id FROM public.space_members WHERE user_id = auth.uid() AND status = 'ACTIVE'
    )
);
