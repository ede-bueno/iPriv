package com.example.priv.data.seed

import com.example.priv.data.dao.*
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.*
import com.example.priv.data.model.ScrapbookItemType
import com.example.priv.data.model.ScrapbookPageItem
import com.example.priv.data.model.SpaceType
import com.example.priv.data.model.SyncStatus
import java.util.UUID

object SeedData {

    suspend fun checkAndSeedIfEmpty(db: PrivDatabase) {
        val existing = db.memoryDao().getAllMemoriesList()
        if (existing.isEmpty()) {
            populateSeedData(
                memoryDao = db.memoryDao(),
                personDao = db.personDao(),
                groupDao = db.groupDao(),
                momentDao = db.momentDao(),
                collectionDao = db.collectionDao(),
                tagDao = db.tagDao(),
                spaceDao = db.spaceDao(),
                timeCapsuleDao = db.timeCapsuleDao(),
                scrapbookDao = db.scrapbookDao(),
                audioMemoryDao = db.audioMemoryDao()
            )
        } else {
            if (db.audioMemoryDao().getAllAudioMemoriesDirect().isEmpty()) {
                seedAudioMemories(db.audioMemoryDao())
            }
            ensureScrapbooksPopulated(db.scrapbookDao())
        }
    }

    suspend fun populateDefaultSpaceOnly(
        spaceDao: SpaceDao
    ) {
        val defaultSpaceId = "default_space"
        spaceDao.insertSpace(
            SpaceEntity(
                id = defaultSpaceId,
                name = "Meu GuardDay",
                description = "Espaço pessoal de memórias atemporais",
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
    }

    suspend fun populateSeedData(
        memoryDao: MemoryDao,
        personDao: PersonDao,
        groupDao: GroupDao,
        momentDao: MomentDao,
        collectionDao: CollectionDao,
        tagDao: TagDao,
        spaceDao: SpaceDao,
        timeCapsuleDao: TimeCapsuleDao,
        scrapbookDao: ScrapbookDao,
        audioMemoryDao: AudioMemoryDao
    ) {
        val defaultSpaceId = "default_space"

        // 1. Default Space
        spaceDao.insertSpace(
            SpaceEntity(
                id = defaultSpaceId,
                name = "Meu GuardDay",
                description = "Espaço seguro de memórias e conexões",
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

        // 2. Persons (Amigos, Família, Colegas)
        val heloId = "seed_person_helo"
        personDao.insertPerson(
            PersonEntity(
                id = heloId,
                spaceId = defaultSpaceId,
                name = "Helô",
                nickname = "Helô Bff",
                relationship = "Melhor Amiga",
                colorHex = "#FF4B72",
                avatarUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                bioNote = "Parceira de todas as risadas e áudios de 3 minutos",
                createdAt = System.currentTimeMillis() - 86400000L * 30
            )
        )

        val gabrielId = "seed_person_gabriel"
        personDao.insertPerson(
            PersonEntity(
                id = gabrielId,
                spaceId = defaultSpaceId,
                name = "Gabriel",
                nickname = "Biel",
                relationship = "Irmão",
                colorHex = "#8E3BEE",
                avatarUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                bioNote = "Sempre mandando memes sem sentido no meio da aula",
                createdAt = System.currentTimeMillis() - 86400000L * 40
            )
        )

        val lucasId = "seed_person_lucas"
        personDao.insertPerson(
            PersonEntity(
                id = lucasId,
                spaceId = defaultSpaceId,
                name = "Lucas",
                nickname = "Lukinhas",
                relationship = "Turma do 3º ano",
                colorHex = "#00F5D4",
                avatarUri = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=200",
                bioNote = "Toca violão na hora do intervalo e grava sunsets",
                createdAt = System.currentTimeMillis() - 86400000L * 25
            )
        )

        val maeId = "seed_person_mae"
        personDao.insertPerson(
            PersonEntity(
                id = maeId,
                spaceId = defaultSpaceId,
                name = "Mãe",
                nickname = "Dona Ana",
                relationship = "Família",
                colorHex = "#FFB703",
                avatarUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200",
                bioNote = "Áudios carinhosos cobrando para levar o casaco e não esquecer as chaves",
                createdAt = System.currentTimeMillis() - 86400000L * 60
            )
        )

        // 3. Groups
        val bffGroupId = "seed_group_bff"
        groupDao.insertGroup(
            GroupEntity(
                id = bffGroupId,
                spaceId = defaultSpaceId,
                name = "Melhores Amigos",
                description = "Galera de sempre, papo diário e fofocas",
                colorHex = "#FF4B72",
                iconName = "Favorite"
            )
        )
        groupDao.insertGroupPersonCrossRef(GroupPersonCrossRef(bffGroupId, heloId))
        groupDao.insertGroupPersonCrossRef(GroupPersonCrossRef(bffGroupId, lucasId))

        val terceiraoGroupId = "seed_group_terceirao"
        groupDao.insertGroup(
            GroupEntity(
                id = terceiraoGroupId,
                spaceId = defaultSpaceId,
                name = "Turma do 3º Ano",
                description = "Provas, formaturas e resenhas do colégio",
                colorHex = "#8E3BEE",
                iconName = "School"
            )
        )
        groupDao.insertGroupPersonCrossRef(GroupPersonCrossRef(terceiraoGroupId, heloId))
        groupDao.insertGroupPersonCrossRef(GroupPersonCrossRef(terceiraoGroupId, gabrielId))
        groupDao.insertGroupPersonCrossRef(GroupPersonCrossRef(terceiraoGroupId, lucasId))

        // 4. Moments
        val anivId = "seed_moment_aniv"
        momentDao.insertMoment(
            MomentEntity(
                id = anivId,
                spaceId = defaultSpaceId,
                title = "Aniversário Surpresa",
                description = "Festa na garagem com bolo de coxinha e galera reunida",
                date = "15/05/2026",
                category = "Festa",
                colorHex = "#FF4B72"
            )
        )

        val praiaId = "seed_moment_praia"
        momentDao.insertMoment(
            MomentEntity(
                id = praiaId,
                spaceId = defaultSpaceId,
                title = "Viagem pra Praia 2025",
                description = "Fim de semana em Ubatuba sem sinal e muito sol",
                date = "20/01/2025",
                category = "Viagem",
                colorHex = "#00F5D4"
            )
        )

        val madrugadasId = "seed_moment_madrugadas"
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

        // 5. Collections
        val amigosColId = "seed_col_amigos"
        collectionDao.insertCollection(
            CollectionEntity(
                id = amigosColId,
                spaceId = defaultSpaceId,
                title = "Melhores Amigos",
                description = "Os áudios e fotos que tiram qualquer um do tédio",
                iconName = "Favorite",
                colorHex = "#FF4B72"
            )
        )

        val audiosClassicosColId = "seed_col_audios"
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

        val fotosColId = "seed_col_fotos"
        collectionDao.insertCollection(
            CollectionEntity(
                id = fotosColId,
                spaceId = defaultSpaceId,
                title = "Scraps & Colagens",
                description = "Fotos com metadados de viagens e encontros",
                iconName = "Collections",
                colorHex = "#00D2D3"
            )
        )

        // 6. Tags
        val tagEngracadoId = "seed_tag_engracado"
        tagDao.insertTag(TagEntity(id = tagEngracadoId, spaceId = defaultSpaceId, name = "Engraçado"))

        val tagConselhoId = "seed_tag_conselho"
        tagDao.insertTag(TagEntity(id = tagConselhoId, spaceId = defaultSpaceId, name = "Conselho"))

        val tagVeraoId = "seed_tag_verao"
        tagDao.insertTag(TagEntity(id = tagVeraoId, spaceId = defaultSpaceId, name = "Verão"))

        val tagMusicaId = "seed_tag_musica"
        tagDao.insertTag(TagEntity(id = tagMusicaId, spaceId = defaultSpaceId, name = "Música"))

        // 7. Memories com Metadados e Multimídia
        val now = System.currentTimeMillis()

        // Memória 1 - Áudio da Helô
        val m1Id = "seed_mem_1"
        val m1Time = now - (86400000L * 2)
        memoryDao.insertMemory(
            MemoryEntity(
                id = m1Id,
                spaceId = defaultSpaceId,
                title = "Helô Rindo do Tombo no Recreio",
                note = "Sempre que estou triste eu escuto essa risada contagiante da Helô!",
                timestamp = m1Time,
                source = "WhatsApp",
                isFavorite = true,
                primaryPersonId = heloId,
                momentId = madrugadasId,
                collectionId = amigosColId,
                tags = "família,amigos,engraçado"
            )
        )
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
        memoryDao.insertGroupCrossRef(MemoryGroupCrossRef(m1Id, bffGroupId))
        memoryDao.insertMomentCrossRef(MemoryMomentCrossRef(m1Id, madrugadasId))
        memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(m1Id, amigosColId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m1Id, tagEngracadoId))

        // Memória 2 - Foto e violão com Lucas na praia (com Metadado EXIF simulado)
        val m2Id = "seed_mem_2"
        val m2Time = now - (86400000L * 15) // 15 dias atrás
        memoryDao.insertMemory(
            MemoryEntity(
                id = m2Id,
                spaceId = defaultSpaceId,
                title = "Pôr do Sol e Violão na Praia",
                note = "Lucas treinando a música acústica nova na beira do mar antes de anoitecer.",
                timestamp = m2Time,
                source = "Galeria (EXIF)",
                isFavorite = true,
                primaryPersonId = lucasId,
                momentId = praiaId,
                collectionId = fotosColId,
                tags = "viagem,praia,música"
            )
        )
        memoryDao.insertAttachment(
            MemoryAttachmentEntity(
                id = UUID.randomUUID().toString(),
                memoryId = m2Id,
                type = "IMAGE",
                uri = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                fileName = "IMG_20260120_183421.jpg",
                fileSize = 1845000L,
                width = 1920,
                height = 1080,
                summary = "Pôr do sol dourado na praia de Ubatuba",
                transcription = "Data original EXIF: 20/01/2026 18:34"
            )
        )
        memoryDao.insertAttachment(
            MemoryAttachmentEntity(
                id = UUID.randomUUID().toString(),
                memoryId = m2Id,
                type = "AUDIO",
                uri = "demo_audio_lucas.ogg",
                fileName = "AUD-20260120-WA0002.mp3",
                durationMs = 54000L,
                fileSize = 420000L,
                waveformData = "0.1,0.3,0.6,0.8,0.5,0.4,0.7,0.9,0.8,0.5,0.3,0.6,0.4,0.2",
                transcription = "[Acordes de violão acústico e som de ondas]",
                summary = "Trecho instrumental gravado na beira do mar"
            )
        )
        memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(m2Id, lucasId))
        memoryDao.insertMomentCrossRef(MemoryMomentCrossRef(m2Id, praiaId))
        memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(m2Id, fotosColId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m2Id, tagVeraoId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m2Id, tagMusicaId))

        // Memória 3 - Gabriel teoria de jogos
        val m3Id = "seed_mem_3"
        val m3Time = now - (86400000L * 5)
        memoryDao.insertMemory(
            MemoryEntity(
                id = m3Id,
                spaceId = defaultSpaceId,
                title = "Biel Explicando a Teoria de Jogos",
                note = "Áudio de 2 minutos que parece uma palestra de física quântica",
                timestamp = m3Time,
                source = "WhatsApp",
                isFavorite = false,
                primaryPersonId = gabrielId,
                momentId = madrugadasId,
                tags = "trabalho,estudo,ideia"
            )
        )
        memoryDao.insertAttachment(
            MemoryAttachmentEntity(
                id = UUID.randomUUID().toString(),
                memoryId = m3Id,
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
        memoryDao.insertPersonCrossRef(MemoryPersonCrossRef(m3Id, gabrielId))
        memoryDao.insertMomentCrossRef(MemoryMomentCrossRef(m3Id, madrugadasId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m3Id, tagEngracadoId))

        // Memória 4 - Mãe
        val m4Id = "seed_mem_4"
        val m4Time = now - (86400000L * 1)
        memoryDao.insertMemory(
            MemoryEntity(
                id = m4Id,
                spaceId = defaultSpaceId,
                title = "Mãe Lembra de Levar o Casaco",
                note = "O clássico 'Leva o casaco que vai esfriar à noite'",
                timestamp = m4Time,
                source = "WhatsApp",
                isFavorite = true,
                primaryPersonId = maeId,
                collectionId = audiosClassicosColId
            )
        )
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
        memoryDao.insertCollectionCrossRef(MemoryCollectionCrossRef(m4Id, audiosClassicosColId))
        memoryDao.insertTagCrossRef(MemoryTagCrossRef(m4Id, tagConselhoId))

        // 8. Cápsulas do Tempo (Time Capsules)
        val capsule1Id = "seed_capsule_1"
        val unlockFormatura = now + (86400000L * 60) // Abre em 60 dias
        timeCapsuleDao.insertCapsule(
            TimeCapsuleEntity(
                id = capsule1Id,
                title = "Cápsula de Formatura 2026",
                note = "Recortes da turma, promessas do 3º ano e áudios que só serão ouvidos na festa de formatura!",
                createdAt = now - (86400000L * 10),
                unlockTimestamp = unlockFormatura,
                isLocked = true,
                memoryIdsJson = "[\"$m1Id\",\"$m2Id\",\"$m3Id\"]",
                mediaItemsJson = "[{\"id\":\"c1\",\"type\":\"AUDIO\",\"title\":\"Discurso de despedida\",\"extraMetadata\":\"Gravação de voz secreta (3:45)\"},{\"id\":\"c2\",\"type\":\"IMAGE\",\"title\":\"Foto da Turma no Pátio\",\"extraMetadata\":\"Foto coletiva analógica\"},{\"id\":\"c3\",\"type\":\"COLLAGE\",\"title\":\"Recortes e Adesivos 2026\",\"textContent\":\"Nossos sonhos e promessas de nunca nos esquecermos\"}]",
                targetPersonId = heloId,
                themeColorHex = "#7C4DFF"
            )
        )

        val capsule2Id = "seed_capsule_2"
        val unlockPassado = now - (86400000L * 2) // Já pronta para abrir!
        timeCapsuleDao.insertCapsule(
            TimeCapsuleEntity(
                id = capsule2Id,
                title = "Metas de Verão & Viagem à Praia",
                note = "Cápsula aberta com fotos inesquecíveis da viagem de janeiro.",
                createdAt = now - (86400000L * 90),
                unlockTimestamp = unlockPassado,
                isLocked = false,
                openedAt = now - 86400000L,
                memoryIdsJson = "[\"$m2Id\"]",
                mediaItemsJson = "[{\"id\":\"c4\",\"type\":\"AUDIO\",\"title\":\"Música do Luau\",\"extraMetadata\":\"Acústico ao vivo no violão\"},{\"id\":\"c5\",\"type\":\"IMAGE\",\"title\":\"Pôr do sol em Maresias\",\"extraMetadata\":\"Foto na areia com a galera\"}]",
                targetPersonId = lucasId,
                themeColorHex = "#00D2D3"
            )
        )

        // 9. Álbuns de Histórias & Murais Afetivos Interativos
        ensureScrapbooksPopulated(scrapbookDao)

        // 10. Memórias de Áudio com Metadados e Transcrições associadas (Room)
        seedAudioMemories(audioMemoryDao)
    }

    suspend fun ensureScrapbooksPopulated(scrapbookDao: ScrapbookDao) {
        val now = System.currentTimeMillis()
        val bestFriendsItems = listOf(
            ScrapbookPageItem(
                id = "bf_slot_1",
                type = ScrapbookItemType.ACTION_SLOT,
                title = "Foto com Meu Melhor Amigo(a) 📸",
                actionPrompt = "Toque aqui para escolher a melhor foto de vocês juntos!",
                rotationDeg = -2.5f
            ),
            ScrapbookPageItem(
                id = "bf_note_1",
                type = ScrapbookItemType.NOTE,
                title = "Regra de Ouro da Amizade 💜",
                uriOrContent = "Parceria em todos os momentos: rir junto até a barriga doer, desabafar nas horas difíceis e nunca deixar uma mensagem sem resposta!",
                noteColorHex = "#FFE5EC",
                authorOrPerson = "Diário Afetivo",
                rotationDeg = 1.8f
            ),
            ScrapbookPageItem(
                id = "bf_slot_2",
                type = ScrapbookItemType.ACTION_SLOT,
                title = "O Momento Mais Engraçado 😂",
                actionPrompt = "Toque para colar aquela foto ou meme que sempre faz a gente gargalhar!",
                rotationDeg = 2.0f
            ),
            ScrapbookPageItem(
                id = "bf_note_2",
                type = ScrapbookItemType.NOTE,
                title = "Trilha Sonora da Amizade 🎵",
                uriOrContent = "Aquela música que quando toca no carro ou na playlist, todo mundo canta junto a plenos pulmões!",
                noteColorHex = "#FFF9DB",
                authorOrPerson = "Nossa Canção",
                rotationDeg = -1.5f
            ),
            ScrapbookPageItem(
                id = "bf_slot_3",
                type = ScrapbookItemType.ACTION_SLOT,
                title = "Nossa Próxima Aventura ✈️",
                actionPrompt = "Toque para registrar a viagem, show ou rolê que vamos realizar juntos em breve!",
                rotationDeg = 1.2f
            )
        )

        val summerItems = listOf(
            ScrapbookPageItem(
                id = "summer_slot_1",
                type = ScrapbookItemType.ACTION_SLOT,
                title = "Pôr do Sol Perfeito 🌅",
                actionPrompt = "Toque para adicionar uma foto do fim de tarde na praia!",
                rotationDeg = -1.8f
            ),
            ScrapbookPageItem(
                id = "summer_note_1",
                type = ScrapbookItemType.NOTE,
                title = "Lembrança de Verão ☀️",
                uriOrContent = "Água de coco gelada, pés descalços na areia, cheiro de protetor solar e conversas que duravam até tarde.",
                noteColorHex = "#E3FAFC",
                authorOrPerson = "Lembrança de Viagem",
                rotationDeg = 2.2f
            ),
            ScrapbookPageItem(
                id = "summer_slot_2",
                type = ScrapbookItemType.ACTION_SLOT,
                title = "A Galera Toda Reunida 🏖️",
                actionPrompt = "Toque para adicionar a foto de todo mundo se divertindo na areia!",
                rotationDeg = 1.5f
            )
        )

        val scrap1 = scrapbookDao.getScrapbookById("seed_scrapbook_1")
        if (scrap1 == null) {
            scrapbookDao.insertScrapbook(
                ScrapbookEntity(
                    id = "seed_scrapbook_1",
                    title = "Verão Inesquecível & Praia 🌊",
                    description = "Mural com fotos da viagem, brisa do mar e risadas na areia.",
                    styleTheme = "VINTAGE_POLAROID",
                    coverEmojiOrIcon = "🌊",
                    stickersJson = "[\"🏖️\",\"☀️\",\"📸\",\"✨\",\"🏄\"]",
                    backgroundPattern = "PASTEL_CREAM",
                    itemsJson = ScrapbookPageItem.serializeList(summerItems),
                    createdAt = now - (86400000L * 14),
                    updatedAt = now - (86400000L * 1)
                )
            )
        } else if (scrap1.itemsJson.isBlank() || scrap1.itemsJson == "[]") {
            scrapbookDao.updateScrapbook(
                scrap1.copy(
                    itemsJson = ScrapbookPageItem.serializeList(summerItems),
                    styleTheme = "VINTAGE_POLAROID",
                    backgroundPattern = "PASTEL_CREAM"
                )
            )
        }

        val scrap2 = scrapbookDao.getScrapbookById("seed_scrapbook_2")
        if (scrap2 == null) {
            scrapbookDao.insertScrapbook(
                ScrapbookEntity(
                    id = "seed_scrapbook_2",
                    title = "Diário Visual dos Melhores Amigos 💜",
                    description = "Mural afetivo com fotos, momentos engraçados e memórias da amizade.",
                    styleTheme = "SOFT_LAVENDER",
                    coverEmojiOrIcon = "👯",
                    stickersJson = "[\"💜\",\"👯\",\"✨\",\"🍕\",\"🎵\"]",
                    backgroundPattern = "SOFT_LAVENDER",
                    itemsJson = ScrapbookPageItem.serializeList(bestFriendsItems),
                    createdAt = now - (86400000L * 20),
                    updatedAt = now
                )
            )
        } else if (scrap2.itemsJson.isBlank() || scrap2.itemsJson == "[]") {
            scrapbookDao.updateScrapbook(
                scrap2.copy(
                    itemsJson = ScrapbookPageItem.serializeList(bestFriendsItems),
                    styleTheme = "SOFT_LAVENDER",
                    coverEmojiOrIcon = "👯",
                    backgroundPattern = "SOFT_LAVENDER"
                )
            )
        }
    }

    suspend fun seedAudioMemories(audioMemoryDao: AudioMemoryDao) {
        val now = System.currentTimeMillis()
        val audios = listOf(
            AudioMemoryEntity(
                id = "seed_audio_1",
                title = "Mensagem carinhosa de aniversário da Vovó 🎂",
                fileName = "vovo_aniversario_2026.m4a",
                filePath = "/data/user/0/com.example/files/local_audio_vault/vovo_aniversario_2026.m4a",
                fileUri = "content://com.example.provider/audio/vovo_aniversario_2026",
                mimeType = "audio/m4a",
                fileSize = 642100L, // ~627 KB
                durationMs = 38500L, // 38.5 segundos
                sampleRate = 44100,
                channels = 1,
                bitrate = 128000,
                waveformData = "12,25,48,65,72,50,30,15,45,80,95,78,60,40,20,55,85,90,70,45,25,10",
                audioChecksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                transcriptionText = "\"Oi meu amorzinho, a vovó tá ligando pra te desejar um feliz aniversário! Que Deus te abençoe e ilumine muito os seus passos. Fiz aquele bolo de cenoura com calda de chocolate que você adora pra gente comer mais tarde! Um beijo bem grande da vó!\"",
                transcriptionStatus = "COMPLETED",
                transcriptionLanguage = "pt-BR",
                transcriptionConfidence = 0.99f,
                transcriptionEngine = "LOCAL_AI",
                summary = "Vovó parabenizando pelo aniversário e avisando sobre o bolo de cenoura com chocolate.",
                tags = "família,aniversário,vovó,amor",
                speakerName = "Vovó Helena",
                linkedMemoryId = "seed_mem_audio_1",
                isFavorite = true,
                source = "WHATSAPP",
                createdAt = now - (86400000L * 3),
                updatedAt = now - (86400000L * 3)
            ),
            AudioMemoryEntity(
                id = "seed_audio_2",
                title = "Ideia de projeto pro GuardDay e Cápsula do Tempo ⏳",
                fileName = "ideia_guardday_capsule.m4a",
                filePath = "/data/user/0/com.example/files/local_audio_vault/ideia_guardday_capsule.m4a",
                fileUri = "content://com.example.provider/audio/ideia_guardday_capsule",
                mimeType = "audio/m4a",
                fileSize = 884200L, // ~863 KB
                durationMs = 54200L, // 54.2 segundos
                sampleRate = 44100,
                channels = 1,
                bitrate = 128000,
                waveformData = "10,20,35,50,65,80,85,75,60,40,30,50,70,88,92,80,65,45,30,15",
                audioChecksum = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e",
                transcriptionText = "\"Nota mental rápida: Tive uma ideia incrível para a Cápsula do Tempo no GuardDay! A gente pode implementar um temporizador com animação de fechadura e lacre digital. Assim as fotos, áudios e transcrições ficam guardados em cofre blindado até a data certa de abertura!\"",
                transcriptionStatus = "COMPLETED",
                transcriptionLanguage = "pt-BR",
                transcriptionConfidence = 0.97f,
                transcriptionEngine = "LOCAL_AI",
                summary = "Ideia para aprimorar a Cápsula do Tempo com trava e animação de cofre digital.",
                tags = "trabalho,ideia,cápsula,guardday,tecnologia",
                speakerName = "Você",
                linkedMemoryId = null,
                isFavorite = true,
                source = "MICROPHONE",
                createdAt = now - (86400000L * 7),
                updatedAt = now - (86400000L * 7)
            ),
            AudioMemoryEntity(
                id = "seed_audio_3",
                title = "Áudio hilário sobre o churrasco da turma 🥩😂",
                fileName = "audio_churrasco_galera.m4a",
                filePath = "/data/user/0/com.example/files/local_audio_vault/audio_churrasco_galera.m4a",
                fileUri = "content://com.example.provider/audio/audio_churrasco_galera",
                mimeType = "audio/m4a",
                fileSize = 352400L, // ~344 KB
                durationMs = 21800L, // 21.8 segundos
                sampleRate = 44100,
                channels = 1,
                bitrate = 128000,
                waveformData = "18,40,75,95,100,85,60,35,80,95,90,70,40,25,60,85,70,30,15",
                audioChecksum = "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae",
                transcriptionText = "\"Mano, você não tem noção do que o Lucas fez kkkk! O copo de suco de maracujá virou certinho dentro da mochila aberta do Pedro. Na hora que ele percebeu a turma inteira rachou o bico de rir no quintal!\"",
                transcriptionStatus = "COMPLETED",
                transcriptionLanguage = "pt-BR",
                transcriptionConfidence = 0.96f,
                transcriptionEngine = "LOCAL_AI",
                summary = "História engraçada do suco de maracujá caindo na mochila durante o churrasco.",
                tags = "amigos,churrasco,risadas,memes",
                speakerName = "Carlos Mendes",
                linkedMemoryId = "seed_mem_audio_3",
                isFavorite = false,
                source = "WHATSAPP",
                createdAt = now - (86400000L * 12),
                updatedAt = now - (86400000L * 12)
            ),
            AudioMemoryEntity(
                id = "seed_audio_4",
                title = "Gravação de ensaio de violão na praia 🏖️🎸",
                fileName = "ensaio_acustico_praia.m4a",
                filePath = "/data/user/0/com.example/files/local_audio_vault/ensaio_acustico_praia.m4a",
                fileUri = "content://com.example.provider/audio/ensaio_acustico_praia",
                mimeType = "audio/m4a",
                fileSize = 1240000L, // ~1.2 MB
                durationMs = 76000L, // 1m 16s
                sampleRate = 44100,
                channels = 2,
                bitrate = 128000,
                waveformData = "15,30,45,60,70,80,75,65,55,70,85,90,80,60,40,30,50,75,85,60,40,20",
                audioChecksum = "7b52009b64fd0a2a49e6d8a939753077792b0554dad5153b0ff7c1e9009f4ccf",
                transcriptionText = "\"[Acordes de violão acústico e som de ondas no fundo] 'Vou deixar a vida me levar... pra onde o vento soprar'. Galera cantando junto em volta da fogueira enquanto o sol se põe.\"",
                transcriptionStatus = "COMPLETED",
                transcriptionLanguage = "pt-BR",
                transcriptionConfidence = 0.92f,
                transcriptionEngine = "LOCAL_AI",
                summary = "Música acústica no violão com a galera reunida na praia ao entardecer.",
                tags = "música,violão,praia,viagem",
                speakerName = "Juliana Costa",
                linkedMemoryId = null,
                isFavorite = true,
                source = "FILE_IMPORT",
                createdAt = now - (86400000L * 18),
                updatedAt = now - (86400000L * 18)
            )
        )

        audioMemoryDao.insertAudioMemories(audios)
    }
}
