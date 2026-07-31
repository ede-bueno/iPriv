package com.example.priv.data.seed

import com.example.priv.data.dao.*
import com.example.priv.data.entity.*
import com.example.priv.data.model.SpaceType
import com.example.priv.data.model.SyncStatus
import java.util.UUID

object SeedData {

    suspend fun populateSeedData(
        memoryDao: MemoryDao,
        personDao: PersonDao,
        momentDao: MomentDao,
        collectionDao: CollectionDao,
        tagDao: TagDao,
        spaceDao: SpaceDao
    ) {
        val defaultSpaceId = "default_space"

        // Default Personal Space
        spaceDao.insertSpace(
            SpaceEntity(
                id = defaultSpaceId,
                name = "Meu Priv",
                description = "Espaço pessoal de memórias",
                type = SpaceType.PERSONAL,
                ownerUserId = "local_user",
                syncStatus = SyncStatus.LOCAL_ONLY
            )
        )

        spaceDao.insertMember(
            SpaceMemberEntity(
                spaceId = defaultSpaceId,
                userId = "local_user"
            )
        )

        // Persons
        val heloId = UUID.randomUUID().toString()
        personDao.insertPerson(
            PersonEntity(
                id = heloId,
                spaceId = defaultSpaceId,
                name = "Helô",
                nickname = "Helô Bff",
                relationship = "Melhor Amiga",
                colorHex = "#FF4B72",
                bioNote = "Parceira de todas as risadas e áudios de 3 minutos"
            )
        )

        val gabrielId = UUID.randomUUID().toString()
        personDao.insertPerson(
            PersonEntity(
                id = gabrielId,
                spaceId = defaultSpaceId,
                name = "Gabriel",
                nickname = "Biel",
                relationship = "Irmão",
                colorHex = "#8E3BEE",
                bioNote = "Sempre mandando memes sem sentido no meio da aula"
            )
        )

        val lucasId = UUID.randomUUID().toString()
        personDao.insertPerson(
            PersonEntity(
                id = lucasId,
                spaceId = defaultSpaceId,
                name = "Lucas",
                nickname = "Lukinhas",
                relationship = "Turma do 3º ano",
                colorHex = "#00F5D4",
                bioNote = "Toca violão na hora do intervalo"
            )
        )

        val maeId = UUID.randomUUID().toString()
        personDao.insertPerson(
            PersonEntity(
                id = maeId,
                spaceId = defaultSpaceId,
                name = "Mãe",
                nickname = "Dona Ana",
                relationship = "Família",
                colorHex = "#FFB703",
                bioNote = "Áudios carinhosos cobrando para arrumar o quarto"
            )
        )

        // Moments
        val anivId = UUID.randomUUID().toString()
        momentDao.insertMoment(
            MomentEntity(
                id = anivId,
                spaceId = defaultSpaceId,
                title = "Aniversário de 2026",
                description = "Festa surpresa na garagem com bolo de coxinha",
                date = "15/05/2026",
                category = "Festa",
                colorHex = "#FF4B72"
            )
        )

        val praiaId = UUID.randomUUID().toString()
        momentDao.insertMoment(
            MomentEntity(
                id = praiaId,
                spaceId = defaultSpaceId,
                title = "Viagem pra Praia 2025",
                description = "Fim de semana em Ubatuba sem sinal de internet",
                date = "20/01/2025",
                category = "Viagem",
                colorHex = "#00F5D4"
            )
        )

        val madrugadasId = UUID.randomUUID().toString()
        momentDao.insertMoment(
            MomentEntity(
                id = madrugadasId,
                spaceId = defaultSpaceId,
                title = "Conversas da Madrugada",
                description = "Filosofias e risadas quando ninguém conseguia dormir",
                date = "2026",
                category = "Conversas",
                colorHex = "#8E3BEE"
            )
        )

        // Collections
        val amigosColId = UUID.randomUUID().toString()
        collectionDao.insertCollection(
            CollectionEntity(
                id = amigosColId,
                spaceId = defaultSpaceId,
                title = "Melhores Amigos",
                description = "Os áudios que tiram qualquer um do tédio",
                iconName = "Favorite",
                colorHex = "#FF4B72"
            )
        )

        val audiosClassicosColId = UUID.randomUUID().toString()
        collectionDao.insertCollection(
            CollectionEntity(
                id = audiosClassicosColId,
                spaceId = defaultSpaceId,
                title = "Áudios Clássicos",
                description = "Histórias épicas que não podem sumir",
                iconName = "Star",
                colorHex = "#8E3BEE"
            )
        )

        val segredosColId = UUID.randomUUID().toString()
        collectionDao.insertCollection(
            CollectionEntity(
                id = segredosColId,
                spaceId = defaultSpaceId,
                title = "Segredos da Galera",
                description = "Guardado a sete chaves no Priv",
                iconName = "Lock",
                colorHex = "#00F5D4"
            )
        )

        // Tags
        val tagEngracadoId = UUID.randomUUID().toString()
        tagDao.insertTag(TagEntity(id = tagEngracadoId, spaceId = defaultSpaceId, name = "Engraçado"))

        val tagConselhoId = UUID.randomUUID().toString()
        tagDao.insertTag(TagEntity(id = tagConselhoId, spaceId = defaultSpaceId, name = "Conselho"))

        val tagFofocaId = UUID.randomUUID().toString()
        tagDao.insertTag(TagEntity(id = tagFofocaId, spaceId = defaultSpaceId, name = "Fofoca"))

        val tagMusicaId = UUID.randomUUID().toString()
        tagDao.insertTag(TagEntity(id = tagMusicaId, spaceId = defaultSpaceId, name = "Música"))

        // Memory 1 - Helô (Áudio icônico do WhatsApp)
        val m1Id = UUID.randomUUID().toString()
        val m1 = MemoryEntity(
            id = m1Id,
            spaceId = defaultSpaceId,
            title = "Helô Rindo do Tombo no Recreio",
            note = "Sempre que estou triste eu escuto essa risada contagiante da Helô!",
            timestamp = System.currentTimeMillis() - 86400000L * 2, // 2 days ago
            source = "WhatsApp",
            isFavorite = true,
            primaryPersonId = heloId,
            momentId = madrugadasId,
            collectionId = amigosColId
        )
        memoryDao.insertMemory(m1)
        memoryDao.insertAttachment(
            MemoryAttachmentEntity(
                id = UUID.randomUUID().toString(),
                memoryId = m1Id,
                type = "AUDIO",
                uri = "demo_audio_helo.ogg",
                fileName = "PTT-20260729-WA0014.ogg",
                durationMs = 38000L,
                fileSize = 245000L,
                waveformData = "0.2,0.4,0.7,0.9,0.5,0.8,0.3,0.6,0.9,1.0,0.7,0.4,0.8,0.6,0.3,0.5,0.8,0.2",
                transcription = "Mano, você não tem noção do que aconteceu hoje! O suco caiu certinho na mochila dele kkkk",
                summary = "Risadas da Helô sobre o incidente do sorvete no recreio"
            )
        )
        memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(m1Id, heloId))
        memoryDao.insertMomentCrossRef(MemoryMomentCrossRef(m1Id, madrugadasId))
        memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(m1Id, amigosColId))
        memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(m1Id, audiosClassicosColId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m1Id, tagEngracadoId))

        // Memory 2 - Gabriel
        val m2Id = UUID.randomUUID().toString()
        val m2 = MemoryEntity(
            id = m2Id,
            spaceId = defaultSpaceId,
            title = "Biel Explicando a Teoria de Jogos",
            note = "Áudio de 2 minutos que parece uma palestra de física quântica",
            timestamp = System.currentTimeMillis() - 86400000L * 5,
            source = "WhatsApp",
            isFavorite = false,
            primaryPersonId = gabrielId,
            momentId = madrugadasId
        )
        memoryDao.insertMemory(m2)
        memoryDao.insertAttachment(
            MemoryAttachmentEntity(
                id = UUID.randomUUID().toString(),
                memoryId = m2Id,
                type = "AUDIO",
                uri = "demo_audio_gabriel.ogg",
                fileName = "PTT-20260726-WA0008.ogg",
                durationMs = 112000L,
                fileSize = 680000L,
                waveformData = "0.3,0.5,0.4,0.6,0.5,0.7,0.8,0.6,0.5,0.4,0.6,0.5,0.3,0.4,0.5,0.6",
                transcription = "Então se você analisar o chefe final, a rota da esquerda corta 30 segundos do speedrun...",
                summary = "Dicas de estratégia de jogo pelo Gabriel"
            )
        )
        memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(m2Id, gabrielId))
        memoryDao.insertMomentCrossRef(MemoryMomentCrossRef(m2Id, madrugadasId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m2Id, tagEngracadoId))

        // Memory 3 - Lucas
        val m3Id = UUID.randomUUID().toString()
        val m3 = MemoryEntity(
            id = m3Id,
            spaceId = defaultSpaceId,
            title = "Ensaio de Violão na Praia",
            note = "Lucas treinando a música nova antes do pôr do sol",
            timestamp = System.currentTimeMillis() - 86400000L * 15,
            source = "Áudio do Aparelho",
            isFavorite = true,
            primaryPersonId = lucasId,
            momentId = praiaId,
            collectionId = amigosColId
        )
        memoryDao.insertMemory(m3)
        memoryDao.insertAttachment(
            MemoryAttachmentEntity(
                id = UUID.randomUUID().toString(),
                memoryId = m3Id,
                type = "AUDIO",
                uri = "demo_audio_lucas.ogg",
                fileName = "AUD-20260715-WA0002.mp3",
                durationMs = 54000L,
                fileSize = 420000L,
                waveformData = "0.1,0.3,0.6,0.8,0.5,0.4,0.7,0.9,0.8,0.5,0.3,0.6,0.4,0.2",
                transcription = "[Acordes de violão acústico e praia ao fundo]",
                summary = "Trecho instrumental gravado na praia"
            )
        )
        memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(m3Id, lucasId))
        memoryDao.insertMomentCrossRef(MemoryMomentCrossRef(m3Id, praiaId))
        memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(m3Id, amigosColId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m3Id, tagMusicaId))

        // Memory 4 - Mãe
        val m4Id = UUID.randomUUID().toString()
        val m4 = MemoryEntity(
            id = m4Id,
            spaceId = defaultSpaceId,
            title = "Mãe Lembra de Levar o Casaco",
            note = "O clássico 'Leva o casaco que vai esfriar'",
            timestamp = System.currentTimeMillis() - 86400000L * 1,
            source = "WhatsApp",
            isFavorite = false,
            primaryPersonId = maeId,
            collectionId = segredosColId
        )
        memoryDao.insertMemory(m4)
        memoryDao.insertAttachment(
            MemoryAttachmentEntity(
                id = UUID.randomUUID().toString(),
                memoryId = m4Id,
                type = "AUDIO",
                uri = "demo_audio_mae.ogg",
                fileName = "PTT-20260730-WA0001.ogg",
                durationMs = 18000L,
                fileSize = 120000L,
                waveformData = "0.4,0.6,0.5,0.7,0.6,0.4,0.3,0.5,0.4,0.2",
                transcription = "Filho, não esquece de pegar a chave e leva uma blusa porque o tempo virou!",
                summary = "Aviso carinhoso da mãe sobre o tempo"
            )
        )
        memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(m4Id, maeId))
        memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(m4Id, segredosColId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m4Id, tagConselhoId))
    }
}
