package org.nuxeo;

import java.io.File;
import java.util.List;
import java.util.Random;

import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.NuxeoClient.Builder;
import org.nuxeo.client.objects.Document;
import org.nuxeo.client.objects.blob.FileBlob;
import org.nuxeo.client.objects.upload.BatchUpload;
import org.nuxeo.client.objects.upload.BatchUploadManager;

public class MDGenerator {

	static final String DEFAULT_PATH = "/default-domain/UserWorkspaces/Administrator/";

	public static void main(String[] args) {

		Builder builder = new NuxeoClient.Builder();
		builder.url("http://localhost:8080/nuxeo").authentication("Administrator", "Administrator").schemas("*")
				.connectTimeout(0).timeout(0);
		NuxeoClient nuxeoClient = builder.connect();

//		for (int i = 0; i < 100; i++) {
//			uploadFileDocumentWithBlob(i, nuxeoClient);
//		}

		for (int i = 0; i < 5; i++) {
			uploadSuperFileDocumentWithBlob(i, nuxeoClient);
		}
	}

	protected static void uploadFileDocumentWithBlob(int fileNumber, NuxeoClient nxClient) {
		BatchUploadManager batchUploadManager = nxClient.batchUploadManager();
		BatchUpload batchUpload = batchUploadManager.createBatch();

		File file = new File("../BinaryStore/file_" + fileNumber + ".txt");
		FileBlob blob = new FileBlob(file);
		batchUpload.upload(Integer.toString(fileNumber), blob);

		String remoteTitle = "File-" + fileNumber;
		Document NuxeoDoc = Document.createWithName(remoteTitle, "File");
		NuxeoDoc.setPropertyValue("dc:title", remoteTitle);

		NuxeoDoc.setPropertyValue("file:content", batchUpload.getBatchBlob(Integer.toString(fileNumber)));
		NuxeoDoc = nxClient.repository().createDocumentByPath(DEFAULT_PATH, NuxeoDoc);
	}

	protected static void uploadSuperFileDocumentWithBlob(int fileNumber, NuxeoClient nxClient) {
		Random random = new Random();
		int contentNum = random.nextInt(99);
		int customNum = random.nextInt(99);

		BatchUploadManager batchUploadManager = nxClient.batchUploadManager();
		BatchUpload batchUpload = batchUploadManager.createBatch();

		File contentFile = new File("../BinaryStore/file_" + contentNum + ".txt");
		FileBlob contentBlob = new FileBlob(contentFile);

		File customFile = new File("../BinaryStore/file_" + customNum + ".txt");
		FileBlob customBlob = new FileBlob(customFile);

		batchUpload.upload(Integer.toString(0), contentBlob);
		batchUpload.upload(Integer.toString(1),  customBlob);

		String remoteTitle = "SuperFile-" + fileNumber;
		Document NuxeoDoc = Document.createWithName(remoteTitle, "SuperFile");

		NuxeoDoc.setPropertyValue("dc:title", remoteTitle);
		NuxeoDoc.setPropertyValue("file:content", batchUpload.getBatchBlob(Integer.toString(0)));
		NuxeoDoc.setPropertyValue("superfile:SuperFileBlobs", List.of(batchUpload.getBatchBlob(Integer.toString(1))));
		NuxeoDoc = nxClient.repository().createDocumentByPath(DEFAULT_PATH, NuxeoDoc);
	}

}
