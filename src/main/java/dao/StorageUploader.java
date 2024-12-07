package dao;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

public class StorageUploader {
    private BlobContainerClient containerClient;

    public StorageUploader() {
        this.containerClient = new BlobContainerClientBuilder()
                .connectionString("DefaultEndpointsProtocol=https;AccountName=velasquezcsc311storage;AccountKey=XbKUO1ADZDRYHhyY7KW8gc5UfSkJtBGRNHTvsC9WJH61yz5NXCW0czu68jIq+USSphE/aXD3zoRy+AStSxWqWg==;EndpointSuffix=core.windows.net")
                .containerName("media-files")
                .buildClient();
    }

    public void uploadFile(String filePath, String blobName) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.uploadFromFile(filePath,true);
    }
    public BlobContainerClient getContainerClient(){
        return containerClient;
    }
}