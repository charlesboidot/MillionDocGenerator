package org.nuxeo;

import java.io.File;

import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.NuxeoClient.Builder;
import org.nuxeo.client.objects.Document;
import org.nuxeo.client.objects.blob.FileBlob;
import org.nuxeo.client.objects.upload.BatchUpload;
import org.nuxeo.client.objects.upload.BatchUploadManager;

public class MDGenerator {

	static final String DEFAULT_PATH = "/default-domain/UserWorkspaces/Administrator";

	public static void main(String[] args) {

		Builder builder = new NuxeoClient.Builder();
		builder.url("http://localhost:8080/nuxeo").authentication("Administrator", "Administrator").schemas("*")
				.connectTimeout(0).timeout(0);
		NuxeoClient nuxeoClient = builder.connect();
		File file = new File("../BinaryStore/file0.txt");
		FileBlob blob = new FileBlob(file);
		int i = 0;
		BatchUploadManager batchUploadManager = nuxeoClient.batchUploadManager();
		BatchUpload batchUpload = batchUploadManager.createBatch();
		batchUpload.upload(Integer.toString(i), blob);
		// create document with the blob
		String remoteTitle = "My test title";
		Document threedDoc = Document.createWithName(remoteTitle, "File");
		threedDoc.setPropertyValue("dc:title", remoteTitle);

		threedDoc.setPropertyValue("file:content", batchUpload.getBatchBlob("0"));
		threedDoc = nuxeoClient.repository().createDocumentByPath(DEFAULT_PATH, threedDoc);
		System.out.println("New doc id = " + threedDoc.getId());
	}
}
