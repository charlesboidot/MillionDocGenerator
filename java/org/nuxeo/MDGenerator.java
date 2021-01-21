package org.nuxeo;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.nuxeo.client.NuxeoClient;
import org.nuxeo.client.NuxeoClient.Builder;
import org.nuxeo.client.objects.Document;
import org.nuxeo.client.objects.blob.FileBlob;
import org.nuxeo.client.objects.upload.BatchUpload;
import org.nuxeo.client.objects.upload.BatchUploadManager;

public class MDGenerator {

	static final String DEFAULT_PATH = "/default-domain/workspaces/GarbageCollectorWorkspace/OneMillion";

	static final int NUMBER_OF_THREADS = 10;

	static final int NUMBER_OF_DOCUMENTS = 1000;

	static final int NUMBER_OF_FILES = NUMBER_OF_DOCUMENTS/2;

	static final int NUMBER_OF_SUPER_FILES = 3*NUMBER_OF_DOCUMENTS/10;

	static final int NUMBER_OF_CUSTOM_FILES = 2*NUMBER_OF_DOCUMENTS/10;

	public static void main(String[] args) {

		ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

		long startTime = System.currentTimeMillis();

		Builder builder = new NuxeoClient.Builder();
		builder.url("http://localhost:8080/nuxeo").authentication("Administrator", "Administrator").schemas("*")
				.connectTimeout(0).timeout(0);
		NuxeoClient nuxeoClient = builder.connect();

		for (int i =0; i<NUMBER_OF_THREADS;i++) {
			Runnable worker = new MyThread(i, nuxeoClient);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {}

		System.out.println("Execution time: "+((double) System.currentTimeMillis()-startTime)/1000+" s");
	}

	protected static void uploadFileDocumentWithBlob(int fileNumber, NuxeoClient nxClient) {
		BatchUploadManager batchUploadManager = nxClient.batchUploadManager();

		BatchUpload batchUpload = batchUploadManager.createBatch();

		File file = new File("../BinaryStore/file_" + fileNumber + ".txt");
		FileBlob blob = new FileBlob(file);
		batchUpload.upload(Integer.toString(0), blob);

		String remoteTitle = "File-" + fileNumber;
		Document NuxeoDoc = Document.createWithName(remoteTitle, "File");
		NuxeoDoc.setPropertyValue("dc:title", remoteTitle);

		NuxeoDoc.setPropertyValue("file:content", batchUpload.getBatchBlob(Integer.toString(0)));
		NuxeoDoc = nxClient.repository().createDocumentByPath(DEFAULT_PATH, NuxeoDoc);
	}

	protected static void uploadCustomFileDocumentWithBlob(int fileNumber, NuxeoClient nxClient) {
		Random random = new Random();
		int customNum = random.nextInt(NUMBER_OF_DOCUMENTS/2-1);

		BatchUploadManager batchUploadManager = nxClient.batchUploadManager();
		BatchUpload batchUpload = batchUploadManager.createBatch();

		File file = new File("../BinaryStore/file_" + customNum + ".txt");
		FileBlob blob = new FileBlob(file);
		batchUpload.upload(Integer.toString(0), blob);

		String remoteTitle = "CustomFile-" + fileNumber;
		Document NuxeoDoc = Document.createWithName(remoteTitle, "CustomFile");
		NuxeoDoc.setPropertyValue("dc:title", remoteTitle);

		NuxeoDoc.setPropertyValue("customfile:CustomFileBlob", batchUpload.getBatchBlob(Integer.toString(0)));
		NuxeoDoc = nxClient.repository().createDocumentByPath(DEFAULT_PATH, NuxeoDoc);
	}

	protected static void uploadSuperFileDocumentWithBlob(int fileNumber, NuxeoClient nxClient) {
		Random random = new Random();
		int contentNum = random.nextInt(NUMBER_OF_DOCUMENTS/2-1);
		int customNum = random.nextInt(NUMBER_OF_DOCUMENTS/2-1);

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

	public static class MyThread implements Runnable {

		private final int threadNumber;

		private final NuxeoClient nxClient;

		MyThread(int threadNumber, NuxeoClient nxClient) {
			this.threadNumber = threadNumber;
			this.nxClient = nxClient;
		}

		@Override
		public void run() {

			int startIndexFile = threadNumber*NUMBER_OF_FILES/NUMBER_OF_THREADS;
			int endIndexFile = startIndexFile+NUMBER_OF_FILES/NUMBER_OF_THREADS;

			int startIndexCustomFile = threadNumber*NUMBER_OF_CUSTOM_FILES/NUMBER_OF_THREADS;
			int endIndexCustomFile = startIndexCustomFile+NUMBER_OF_CUSTOM_FILES/NUMBER_OF_THREADS;

			int startIndexSuperFile = threadNumber*NUMBER_OF_SUPER_FILES/NUMBER_OF_THREADS;
			int endIndexSuperFile = startIndexSuperFile+NUMBER_OF_SUPER_FILES/NUMBER_OF_THREADS;
			System.out.println("Init....");
			try {
				for (int i = startIndexFile; i < endIndexFile; i++) {
					uploadFileDocumentWithBlob(i, nxClient);
				}
				for (int i = startIndexCustomFile; i < endIndexCustomFile; i++) {
					uploadCustomFileDocumentWithBlob(i, nxClient);
				}
				for (int i = startIndexSuperFile; i < endIndexSuperFile; i++) {
					uploadSuperFileDocumentWithBlob(i, nxClient);
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
