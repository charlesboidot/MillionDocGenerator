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

	// Configuration

	static final String DEFAULT_PATH = "/default-domain/workspaces/GarbageCollectorWorkspace/OneMillion";

	static final int NUMBER_OF_THREADS = 20;

	static final int TOTAL_OF_DOCUMENTS = 1000000;

	static final int NUMBER_OF_LOOPS = 100;

	static final int NUMBER_OF_FOLDERS = 1000;

	// Used variables

	static final int NUMBER_OF_DOCUMENTS = TOTAL_OF_DOCUMENTS / NUMBER_OF_LOOPS; // Number of documents per loop

	static final int NUMBER_OF_FILES = NUMBER_OF_DOCUMENTS / 2;

	static final int NUMBER_OF_SUPER_FILES = 3 * NUMBER_OF_DOCUMENTS / 10;

	static final int NUMBER_OF_CUSTOM_FILES = 2 * NUMBER_OF_DOCUMENTS / 10;

	public static void main(String[] args) {
		//////////
		Builder builder = new NuxeoClient.Builder();
		builder.url("http://localhost:8080/nuxeo").authentication("Administrator", "Administrator").schemas("*")
				.connectTimeout(0).timeout(0);
		NuxeoClient nuxeoClient = builder.connect();

		long startTime = System.currentTimeMillis();

		//////////

		for (int k = 70; k < NUMBER_OF_LOOPS; k++) {
			if (k == 1) {
				for (int i = 0; i < NUMBER_OF_FOLDERS; i++) {
					Document NuxeoDoc = Document.createWithName("Folder-" + i, "Folder");
					NuxeoDoc = nuxeoClient.repository().createDocumentByPath(DEFAULT_PATH, NuxeoDoc);
				}
	            System.out.println("Folders generated in " + ((double) System.currentTimeMillis() - startTime) / 1000 + " s");
			}

			ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			long loopStartTime = System.currentTimeMillis();

			for (int i = 0; i < NUMBER_OF_THREADS; i++) {
				Runnable worker = new MyThread(k, i, nuxeoClient);
				executor.execute(worker);
			}
			executor.shutdown();
			while (!executor.isTerminated()) {
			}
			System.out.println("\n" + (k + 1) + "% completed in "
					+ ((double) System.currentTimeMillis() - loopStartTime) / 1000 + " s\n");
		}

		System.out.println("Execution time: " + ((double) System.currentTimeMillis() - startTime) / 1000 + " s");
	}

	protected static void uploadFileDocumentWithBlob(int fileNumber, NuxeoClient nxClient) {
		String path = DEFAULT_PATH + "/Folder-" + (fileNumber % NUMBER_OF_FOLDERS);
		BatchUploadManager batchUploadManager = nxClient.batchUploadManager();

		BatchUpload batchUpload = batchUploadManager.createBatch();

		File file = new File("../BinaryStore/file_" + fileNumber + ".txt");
		FileBlob blob = new FileBlob(file);
		batchUpload.upload(Integer.toString(0), blob);

		String remoteTitle = "File-" + fileNumber;
		Document NuxeoDoc = Document.createWithName(remoteTitle, "File");
		NuxeoDoc.setPropertyValue("dc:title", remoteTitle);

		NuxeoDoc.setPropertyValue("file:content", batchUpload.getBatchBlob(Integer.toString(0)));
		NuxeoDoc = nxClient.repository().createDocumentByPath(path, NuxeoDoc);
	}

	protected static void uploadCustomFileDocumentWithBlob(int fileNumber, NuxeoClient nxClient) {
		String path = DEFAULT_PATH + "/Folder-" + (fileNumber % NUMBER_OF_FOLDERS);

		Random random = new Random();
		int customNum = random.nextInt(NUMBER_OF_DOCUMENTS / 2 - 1);

		BatchUploadManager batchUploadManager = nxClient.batchUploadManager();
		BatchUpload batchUpload = batchUploadManager.createBatch();

		File file = new File("../BinaryStore/file_" + customNum + ".txt");
		FileBlob blob = new FileBlob(file);
		batchUpload.upload(Integer.toString(0), blob);

		String remoteTitle = "CustomFile-" + fileNumber;
		Document NuxeoDoc = Document.createWithName(remoteTitle, "CustomFile");
		NuxeoDoc.setPropertyValue("dc:title", remoteTitle);

		NuxeoDoc.setPropertyValue("customfile:CustomFileBlob", batchUpload.getBatchBlob(Integer.toString(0)));
		NuxeoDoc = nxClient.repository().createDocumentByPath(path, NuxeoDoc);
	}

	protected static void uploadSuperFileDocumentWithBlob(int fileNumber, NuxeoClient nxClient) {
		String path = DEFAULT_PATH + "/Folder-" + (fileNumber % NUMBER_OF_FOLDERS);

		Random random = new Random();
		int contentNum = random.nextInt(NUMBER_OF_DOCUMENTS / 2 - 1);
		int customNum = random.nextInt(NUMBER_OF_DOCUMENTS / 2 - 1);

		BatchUploadManager batchUploadManager = nxClient.batchUploadManager();
		BatchUpload batchUpload = batchUploadManager.createBatch();

		File contentFile = new File("../BinaryStore/file_" + contentNum + ".txt");
		FileBlob contentBlob = new FileBlob(contentFile);

		File customFile = new File("../BinaryStore/file_" + customNum + ".txt");
		FileBlob customBlob = new FileBlob(customFile);

		batchUpload.upload(Integer.toString(0), contentBlob);
		batchUpload.upload(Integer.toString(1), customBlob);

		String remoteTitle = "SuperFile-" + fileNumber;
		Document NuxeoDoc = Document.createWithName(remoteTitle, "SuperFile");

		NuxeoDoc.setPropertyValue("dc:title", remoteTitle);
		NuxeoDoc.setPropertyValue("file:content", batchUpload.getBatchBlob(Integer.toString(0)));
		NuxeoDoc.setPropertyValue("superfile:SuperFileBlobs", List.of(batchUpload.getBatchBlob(Integer.toString(1))));
		NuxeoDoc = nxClient.repository().createDocumentByPath(path, NuxeoDoc);
	}

	public static class MyThread implements Runnable {

		private final int threadNumber;

		private final int loopNumber;

		private final NuxeoClient nxClient;

		MyThread(int loopNumber, int threadNumber, NuxeoClient nxClient) {
			this.loopNumber = loopNumber;
			this.threadNumber = threadNumber;
			this.nxClient = nxClient;
		}

		@Override
		public void run() {

			int startIndexFile = (loopNumber * NUMBER_OF_FILES) + threadNumber * NUMBER_OF_FILES / NUMBER_OF_THREADS;
			int endIndexFile = startIndexFile + NUMBER_OF_FILES / NUMBER_OF_THREADS;

			int startIndexCustomFile = (loopNumber * NUMBER_OF_CUSTOM_FILES)
					+ threadNumber * NUMBER_OF_CUSTOM_FILES / NUMBER_OF_THREADS;
			int endIndexCustomFile = startIndexCustomFile + NUMBER_OF_CUSTOM_FILES / NUMBER_OF_THREADS;

			int startIndexSuperFile = (loopNumber * NUMBER_OF_SUPER_FILES)
					+ threadNumber * NUMBER_OF_SUPER_FILES / NUMBER_OF_THREADS;
			int endIndexSuperFile = startIndexSuperFile + NUMBER_OF_SUPER_FILES / NUMBER_OF_THREADS;
			// System.out.println("Initialising thread " + threadNumber);
			try {
				for (int i = startIndexFile; i < endIndexFile; i++) {
					uploadFileDocumentWithBlob(i, nxClient);
				}
				// System.out.println("[Thread " + threadNumber + "] has finished Files
				// generating files from " + startIndexFile + " to " + endIndexFile + ".");
				for (int i = startIndexCustomFile; i < endIndexCustomFile; i++) {
					uploadCustomFileDocumentWithBlob(i, nxClient);
				}
				// System.out.println("[Thread " + threadNumber + "] has finished CustomFiles
				// generating files from " + startIndexCustomFile + " to " + endIndexCustomFile
				// + ".");
				for (int i = startIndexSuperFile; i < endIndexSuperFile; i++) {
					uploadSuperFileDocumentWithBlob(i, nxClient);
				}
				// System.out.println("[Thread " + threadNumber + "] has finished SuperFiles
				// generating files from " + startIndexSuperFile + " to " + endIndexSuperFile +
				// ".");
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
