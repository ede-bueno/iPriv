package com.example.priv.data.model

enum class SyncStatus {
    LOCAL_ONLY,
    PENDING_CREATE,
    PENDING_UPLOAD,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNCING,
    SYNCED,
    SYNC_ERROR,
    CONFLICT
}

enum class SpaceType {
    PERSONAL,
    GROUP,
    EVENT
}

enum class RoleType {
    OWNER,
    EDITOR,
    CONTRIBUTOR,
    VIEWER
}

enum class MemberStatus {
    INVITED,
    ACTIVE,
    REMOVED
}

enum class OperationType {
    CREATE_ENTITY,
    UPDATE_ENTITY,
    DELETE_ENTITY,
    UPLOAD_MEDIA,
    DOWNLOAD_MEDIA,
    DELETE_MEDIA
}

enum class AttachmentSyncStatus {
    LOCAL_ONLY,
    QUEUED,
    UPLOADING,
    UPLOADED,
    AVAILABLE_REMOTE,
    DOWNLOAD_QUEUED,
    DOWNLOADING,
    AVAILABLE_LOCAL,
    UPLOAD_ERROR,
    DOWNLOAD_ERROR,
    PENDING_DELETE
}
