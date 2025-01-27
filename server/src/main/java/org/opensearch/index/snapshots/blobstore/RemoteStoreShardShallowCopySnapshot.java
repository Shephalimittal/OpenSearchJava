/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.snapshots.blobstore;

import org.opensearch.OpenSearchParseException;
import org.opensearch.core.ParseField;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Remote Store based Shard snapshot metadata
 *
 * @opensearch.internal
 */
public class RemoteStoreShardShallowCopySnapshot implements ToXContentFragment {

    private final String snapshot;
    private final String version;
    private final long indexVersion;
    private final long startTime;
    private final long time;
    private final int totalFileCount;
    private final long totalSize;
    private final long primaryTerm;
    private final long commitGeneration;
    private final String remoteStoreRepository;
    private final String repositoryBasePath;
    private final String indexUUID;
    private final List<String> fileNames;

    static final String DEFAULT_VERSION = "1";
    static final String NAME = "name";
    static final String VERSION = "version";
    static final String INDEX_VERSION = "index_version";
    static final String START_TIME = "start_time";
    static final String TIME = "time";

    static final String INDEX_UUID = "index_uuid";

    static final String REMOTE_STORE_REPOSITORY = "remote_store_repository";
    static final String REPOSITORY_BASE_PATH = "remote_store_repository_base_path";
    static final String FILE_NAMES = "file_names";

    static final String PRIMARY_TERM = "primary_term";

    static final String COMMIT_GENERATION = "commit_generation";

    static final String TOTAL_FILE_COUNT = "number_of_files";
    static final String TOTAL_SIZE = "total_size";

    private static final ParseField PARSE_NAME = new ParseField(NAME);
    private static final ParseField PARSE_VERSION = new ParseField(VERSION);
    private static final ParseField PARSE_PRIMARY_TERM = new ParseField(PRIMARY_TERM);
    private static final ParseField PARSE_COMMIT_GENERATION = new ParseField(COMMIT_GENERATION);
    private static final ParseField PARSE_INDEX_VERSION = new ParseField(INDEX_VERSION, "index-version");
    private static final ParseField PARSE_START_TIME = new ParseField(START_TIME);
    private static final ParseField PARSE_TIME = new ParseField(TIME);
    private static final ParseField PARSE_TOTAL_FILE_COUNT = new ParseField(TOTAL_FILE_COUNT);
    private static final ParseField PARSE_TOTAL_SIZE = new ParseField(TOTAL_SIZE);
    private static final ParseField PARSE_INDEX_UUID = new ParseField(INDEX_UUID);
    private static final ParseField PARSE_REMOTE_STORE_REPOSITORY = new ParseField(REMOTE_STORE_REPOSITORY);
    private static final ParseField PARSE_REPOSITORY_BASE_PATH = new ParseField(REPOSITORY_BASE_PATH);
    private static final ParseField PARSE_FILE_NAMES = new ParseField(FILE_NAMES);

    /**
     * Serializes shard snapshot metadata info into JSON
     *
     * @param builder  XContent builder
     * @param params   parameters
     */
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(VERSION, version);
        builder.field(NAME, snapshot);
        builder.field(INDEX_VERSION, indexVersion);
        builder.field(START_TIME, startTime);
        builder.field(TIME, time);
        builder.field(TOTAL_FILE_COUNT, totalFileCount);
        builder.field(TOTAL_SIZE, totalSize);
        builder.field(INDEX_UUID, indexUUID);
        builder.field(REMOTE_STORE_REPOSITORY, remoteStoreRepository);
        builder.field(COMMIT_GENERATION, commitGeneration);
        builder.field(PRIMARY_TERM, primaryTerm);
        builder.field(REPOSITORY_BASE_PATH, repositoryBasePath);
        builder.startArray(FILE_NAMES);
        for (String fileName : fileNames) {
            builder.value(fileName);
        }
        builder.endArray();
        return builder;
    }

    public RemoteStoreShardShallowCopySnapshot(
        String snapshot,
        long indexVersion,
        long primaryTerm,
        long commitGeneration,
        long startTime,
        long time,
        int totalFileCount,
        long totalSize,
        String indexUUID,
        String remoteStoreRepository,
        String repositoryBasePath,
        List<String> fileNames
    ) {
        this.version = DEFAULT_VERSION;
        verifyParameters(
            version,
            snapshot,
            indexVersion,
            primaryTerm,
            commitGeneration,
            indexUUID,
            remoteStoreRepository,
            repositoryBasePath
        );
        this.snapshot = snapshot;
        this.indexVersion = indexVersion;
        this.primaryTerm = primaryTerm;
        this.commitGeneration = commitGeneration;
        this.startTime = startTime;
        this.time = time;
        this.totalFileCount = totalFileCount;
        this.totalSize = totalSize;
        this.indexUUID = indexUUID;
        this.remoteStoreRepository = remoteStoreRepository;
        this.repositoryBasePath = repositoryBasePath;
        this.fileNames = fileNames;
    }

    private RemoteStoreShardShallowCopySnapshot(
        String version,
        String snapshot,
        long indexVersion,
        long primaryTerm,
        long commitGeneration,
        long startTime,
        long time,
        int totalFileCount,
        long totalSize,
        String indexUUID,
        String remoteStoreRepository,
        String repositoryBasePath,
        List<String> fileNames
    ) {
        verifyParameters(
            version,
            snapshot,
            indexVersion,
            primaryTerm,
            commitGeneration,
            indexUUID,
            remoteStoreRepository,
            repositoryBasePath
        );
        this.version = version;
        this.snapshot = snapshot;
        this.indexVersion = indexVersion;
        this.primaryTerm = primaryTerm;
        this.commitGeneration = commitGeneration;
        this.startTime = startTime;
        this.time = time;
        this.totalFileCount = totalFileCount;
        this.totalSize = totalSize;
        this.indexUUID = indexUUID;
        this.remoteStoreRepository = remoteStoreRepository;
        this.repositoryBasePath = repositoryBasePath;
        this.fileNames = fileNames;
    }

    /**
     * Parses shard snapshot metadata
     *
     * @param parser parser
     * @return shard snapshot metadata
     */
    public static RemoteStoreShardShallowCopySnapshot fromXContent(XContentParser parser) throws IOException {
        String snapshot = null;
        String version = null;
        long indexVersion = -1;
        long startTime = 0;
        long time = 0;
        int totalFileCount = 0;
        long totalSize = 0;
        String indexUUID = null;
        String remoteStoreRepository = null;
        String repositoryBasePath = null;
        long primaryTerm = -1;
        long commitGeneration = -1;
        List<String> fileNames = new ArrayList<>();

        if (parser.currentToken() == null) { // fresh parser? move to the first token
            parser.nextToken();
        }
        XContentParser.Token token;
        String currentFieldName = parser.currentName();
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                if (PARSE_NAME.match(currentFieldName, parser.getDeprecationHandler())) {
                    snapshot = parser.text();
                } else if (PARSE_VERSION.match(currentFieldName, parser.getDeprecationHandler())) {
                    version = parser.text();
                } else if (PARSE_INDEX_VERSION.match(currentFieldName, parser.getDeprecationHandler())) {
                    indexVersion = parser.longValue();
                } else if (PARSE_PRIMARY_TERM.match(currentFieldName, parser.getDeprecationHandler())) {
                    primaryTerm = parser.longValue();
                } else if (PARSE_COMMIT_GENERATION.match(currentFieldName, parser.getDeprecationHandler())) {
                    commitGeneration = parser.longValue();
                } else if (PARSE_START_TIME.match(currentFieldName, parser.getDeprecationHandler())) {
                    startTime = parser.longValue();
                } else if (PARSE_TIME.match(currentFieldName, parser.getDeprecationHandler())) {
                    time = parser.longValue();
                } else if (PARSE_TOTAL_FILE_COUNT.match(currentFieldName, parser.getDeprecationHandler())) {
                    totalFileCount = parser.intValue();
                } else if (PARSE_TOTAL_SIZE.match(currentFieldName, parser.getDeprecationHandler())) {
                    totalSize = parser.longValue();
                } else if (PARSE_INDEX_UUID.match(currentFieldName, parser.getDeprecationHandler())) {
                    indexUUID = parser.text();
                } else if (PARSE_REMOTE_STORE_REPOSITORY.match(currentFieldName, parser.getDeprecationHandler())) {
                    remoteStoreRepository = parser.text();
                } else if (PARSE_REPOSITORY_BASE_PATH.match(currentFieldName, parser.getDeprecationHandler())) {
                    repositoryBasePath = parser.text();
                } else {
                    throw new OpenSearchParseException("unknown parameter [{}]", currentFieldName);
                }
            } else if (token == XContentParser.Token.START_ARRAY) {
                if (PARSE_FILE_NAMES.match(currentFieldName, parser.getDeprecationHandler())) {
                    while ((parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        fileNames.add(parser.text());
                    }
                } else {
                    parser.skipChildren();
                }
            } else {
                parser.skipChildren();
            }
        }

        return new RemoteStoreShardShallowCopySnapshot(
            version,
            snapshot,
            indexVersion,
            primaryTerm,
            commitGeneration,
            startTime,
            time,
            totalFileCount,
            totalSize,
            indexUUID,
            remoteStoreRepository,
            repositoryBasePath,
            fileNames
        );
    }

    /**
     * Returns shard primary Term during snapshot creation
     *
     * @return primary Term
     */
    public long getPrimaryTerm() {
        return primaryTerm;
    }

    /**
     * Returns snapshot commit generation
     *
     * @return commit Generation
     */
    public long getCommitGeneration() {
        return commitGeneration;
    }

    /**
     * Returns Index UUID
     *
     * @return index UUID
     */
    public String getIndexUUID() {
        return indexUUID;
    }

    /**
     * Returns Remote Store Repository Name
     *
     * @return remote store Repository Name
     */
    public String getRemoteStoreRepository() {
        return remoteStoreRepository;
    }

    /**
     * Returns Remote Store Repository Base Path
     *
     * @return repository base path
     */
    public String getRepositoryBasePath() {
        return repositoryBasePath;
    }

    /**
     * Returns snapshot name
     *
     * @return snapshot name
     */
    public String snapshot() {
        return snapshot;
    }

    /**
     * Returns list of files in the shard
     *
     * @return list of files
     */

    /**
     * Returns snapshot start time
     */
    public long startTime() {
        return startTime;
    }

    /**
     * Returns snapshot running time
     */
    public long time() {
        return time;
    }

    /**
     * Returns incremental of files that were snapshotted
     */
    public int incrementalFileCount() {
        return 0;
    }

    /**
     * Returns total number of files that are referenced by this snapshot
     */
    public int totalFileCount() {
        return totalFileCount;
    }

    /**
     * Returns incremental of files size that were snapshotted
     */
    public long incrementalSize() {
        return 0;
    }

    /**
     * Returns total size of all files that where snapshotted
     */
    public long totalSize() {
        return totalSize;
    }

    private void verifyParameters(
        String version,
        String snapshot,
        long indexVersion,
        long primaryTerm,
        long commitGeneration,
        String indexUUID,
        String remoteStoreRepository,
        String repositoryBasePath
    ) {
        String exceptionStr = null;
        if (version == null) {
            exceptionStr = "Invalid Version Provided";
        }
        if (snapshot == null) {
            exceptionStr = "Invalid/Missing Snapshot Name";
        }
        if (indexVersion < 0) {
            exceptionStr = "Invalid Index Version";
        }
        if (primaryTerm < 0) {
            exceptionStr = "Invalid Primary Term";
        }
        if (commitGeneration < 0) {
            exceptionStr = "Invalid Commit Generation";
        }
        if (indexUUID == null) {
            exceptionStr = "Invalid/Missing Index UUID";
        }
        if (remoteStoreRepository == null) {
            exceptionStr = "Invalid/Missing Remote Store Repository";
        }
        if (repositoryBasePath == null) {
            exceptionStr = "Invalid/Missing Repository Base Path";
        }
        if (exceptionStr != null) {
            throw new IllegalArgumentException(exceptionStr);
        }
    }

    /**
     * Creates a new instance which has a different name and zero incremental file counts but is identical to this instance in terms of the files
     * it references.
     *
     * @param targetSnapshotName target snapshot name
     * @param startTime          time the clone operation on the repository was started
     * @param time               time it took to create the clone
     */
    public RemoteStoreShardShallowCopySnapshot asClone(String targetSnapshotName, long startTime, long time) {
        return new RemoteStoreShardShallowCopySnapshot(
            targetSnapshotName,
            indexVersion,
            primaryTerm,
            commitGeneration,
            startTime,
            time,
            totalFileCount,
            totalSize,
            indexUUID,
            remoteStoreRepository,
            repositoryBasePath,
            fileNames
        );
    }
}
