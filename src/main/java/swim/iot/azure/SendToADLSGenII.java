package swim.iot.azure;

import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.*;
import swim.iot.util.EnvConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SendToADLSGenII {

//  public DataLakeServiceClient dsClient;
//
//  public SendToADLSGenII(String accountName, String accountKey) {
//    this.dsClient = getDataLakeServiceClient(accountName, accountKey);
//  }

  /**
   * Create DataLakeServiceClient using account key
   * @param accountName
   * @param accountKey
   * @return
   */
  // todo: update accountName & accountKey from Env
  public static DataLakeServiceClient getDataLakeServiceClient
      (String accountName, String accountKey){

    StorageSharedKeyCredential sharedKeyCredential =
        new StorageSharedKeyCredential(accountName, accountKey);

    DataLakeServiceClientBuilder builder = new DataLakeServiceClientBuilder();

    builder.credential(sharedKeyCredential);
    builder.endpoint("https://" + accountName + ".dfs.core.windows.net");

    return builder.buildClient();
  }

  /**
   * A container acts as a file system for your files. You can create one by calling the DataLakeServiceClient.createFileSystem method.
   *
   * This example creates a container named test-system.
   * @param serviceClient
   * @return
   */
  public DataLakeFileSystemClient CreateFileSystem(DataLakeServiceClient serviceClient){

    return serviceClient.createFileSystem(EnvConfig.FILE_SYSTEM);
  }

  /**
   *
   * Create a directory reference by calling the DataLakeFileSystemClient.createDirectory method.
   *
   * This example adds a directory named my-directory to a container, and then adds a sub-directory named my-subdirectory.
   *
   * @param serviceClient
   * @param fileSystemName
   * @return
   */
//  public DataLakeDirectoryClient CreateDirectory
//      (DataLakeServiceClient serviceClient, String fileSystemName){
//
//    DataLakeFileSystemClient fileSystemClient =
//        serviceClient.getFileSystemClient(fileSystemName);
//
//    DataLakeDirectoryClient directoryClient =
//        fileSystemClient.createDirectory("my-directory");
//
//    return directoryClient.createSubdirectory("my-subdirectory");
//  }

  /**
   * First, create a file reference in the target directory by creating an instance of the DataLakeFileClient class. Upload a file by calling the DataLakeFileClient.append method. Make sure to complete the upload by calling the DataLakeFileClient.FlushAsync method.
   *
   * This example uploads a text file to a directory named my-directory.
   * @param fileSystemClient
   * @throws FileNotFoundException
   */
  public void uploadFile(DataLakeFileSystemClient fileSystemClient, String content)
      throws FileNotFoundException {

    DataLakeDirectoryClient directoryClient =
        fileSystemClient.getDirectoryClient("my-directory");

    // todo
    DataLakeFileClient fileClient = directoryClient.createFile("uploaded-file.txt");

//    File file = new File("C:\\Users\\constoso\\mytestfile.txt");

    //   InputStream targetStream = new FileInputStream(file);
//    InputStream targetStream = new BufferedInputStream(new FileInputStream(file));
    InputStream targetStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));


    long fileSize = content.length();

    fileClient.append(targetStream, 0, fileSize);

    fileClient.flush(fileSize);
  }

  /**
   * Use the DataLakeFileClient.uploadFromFile method to upload large files without having to make multiple calls
   * to the DataLakeFileClient.append method.
   *
   * @param fileSystemClient
   * @throws FileNotFoundException
   */
  public void uploadFileBulk(DataLakeFileSystemClient fileSystemClient)
      throws FileNotFoundException{

    DataLakeDirectoryClient directoryClient =
        fileSystemClient.getDirectoryClient("my-directory");

    DataLakeFileClient fileClient = directoryClient.getFileClient("uploaded-file.txt");

    fileClient.uploadFromFile("C:\\Users\\contoso\\mytestfile.txt");

  }

}
