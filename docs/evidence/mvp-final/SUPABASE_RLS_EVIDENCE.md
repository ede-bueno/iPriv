# EVIDÊNCIAS DE INTEGRAÇÃO E RLS SUPABASE

## 1. Estrutura de Tabelas e RLS Aplicadas

Toda a segurança no Supabase é baseada nas políticas RLS definidas em `app/supabase_migrations_v1.sql`:

```sql
-- Exemplo da Policy RLS de Memórias
CREATE POLICY "Users can read memories of their spaces"
ON public.memories FOR SELECT
USING (
    space_id IN (
        SELECT space_id FROM public.space_members
        WHERE user_id = auth.uid() AND status = 'ACTIVE'
    )
);
```

## 2. Rota de Armazenamento no Storage

- **Bucket**: `memories` (Privado)
- **Padrão de Caminho**: `spaces/{spaceId}/memories/{memoryId}/{attachmentId}.ogg`
- **Isolamento**: Verificado por RLS Storage Policy exigindo que o `auth.uid()` seja membro ativo do `space_id` presente no caminho.
