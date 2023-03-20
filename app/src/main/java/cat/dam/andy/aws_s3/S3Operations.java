package cat.dam.andy.aws_s3;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtilityOptions;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

public class S3Operations {
    private String TAG="S3Operations";
    private AmazonS3 s3Client;
    private String bucketName;
    private AWS_KEYS keys= new AWS_KEYS();
    private TransferUtility transferUtility;
    private TransferUtilityOptions transferUtilityOptions;
    private Executor executor = Executors.newSingleThreadExecutor();

    public S3Operations(Context context, String accessKey, String secretKey, String sessionToken , String bucketName) {
        try {
            //if we need credentials to access S3 (in public bucked also we need for object copy or move)
            AWSCredentials credentials = new BasicSessionCredentials(
                    accessKey,
                    secretKey,
                    sessionToken);
            s3Client = new AmazonS3Client(credentials, Region.getRegion(keys.getRegion()));
            //if we don't need credentials to access S3 (for public access AND not object copy or move operations)
//            s3Client = new AmazonS3Client(new AnonymousAWSCredentials(), Region.getRegion(keys.getRegion()));
            this.bucketName = bucketName;
            transferUtilityOptions = new TransferUtilityOptions();
            transferUtilityOptions.setMinimumUploadPartSizeInMB(10); // Set minimum upload part size to 10MB >=5MB
            transferUtilityOptions.setTransferThreadPoolSize(8); // Set transfer thread pool size to 8
            transferUtility = TransferUtility.builder()
                    .s3Client(s3Client)
                    .transferUtilityOptions(transferUtilityOptions)
                    .context(context)
                    .build();
            TransferNetworkLossHandler.getInstance(context);// to avoid network loss message when transfer
        } catch (AmazonServiceException ase) {
            Log.e(TAG, "Error Message: " + ase.getMessage());
            Log.e(TAG, "HTTP Status Code: " + ase.getStatusCode());
            Log.e(TAG, "AWS Error Code: " + ase.getErrorCode());
            Log.e(TAG, "Error Type: " + ase.getErrorType());
            Log.e(TAG, "Request ID: " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            Log.e(TAG, "Error Message: " + ace.getMessage());
        }
        catch (Exception e){
            Log.e(TAG, "Error creating S3Operations: " + e.getMessage());
        }
    }

    public List<String> listObjects(AmazonS3 s3Client,Callback callback) {
        List<String> objectList = new ArrayList<>();
        try {
                ObjectListing objects = s3Client.listObjects(new ListObjectsRequest().withBucketName(bucketName));
                for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                    objectList.add(objectSummary.getKey());
                }
                Log.d(TAG, "Object list from AWS-S3 bucket '" + bucketName + "': " + objectList);
                // Update UI with the results
                // This code runs on the main thread
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 400 || e.getStatusCode() == 403) {
                if (e.getErrorMessage().contains("InvalidAccessKeyId")) {
                    System.out.println("ERROR "+e.getStatusCode()+" - "+ "S3 authentication error: Invalid Access key Id");
                } else if (e.getErrorMessage().contains("SignatureDoesNotMatch")) {
                    System.out.println("ERROR "+e.getStatusCode()+" - "+"S3 authentication error: Secret Access key does not match");
                } else if (e.getErrorMessage().contains("InvalidToken")) {
                    System.out.println("ERROR "+e.getStatusCode()+" - "+"S3 authentication error: Invalid Access key token");
                } else {
                    System.out.println("S3 service error: " + e.getErrorMessage());
                }
            }
            else {
                System.out.println("S3 service error: " + e.getMessage());
            }
        } catch (AmazonClientException e) {
            System.out.println("S3 client error: " + e.getMessage());
        }
        return objectList;
    }

    public void uploadFile(Context context, AmazonS3 s3Client, int resource, String s3Key) {
        File file = getFileFromResource(context, resource);
        TransferObserver observer = transferUtility.upload(bucketName, s3Key, file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "onStateChanged: Uploading '"+s3Key+"' to AWS-S3 bucket '"+bucketName+ "' (id:"+ id + " st:" + state.toString()+")");
                // If upload error, failed or network disconnect
                if(state == TransferState.FAILED || state == TransferState.WAITING_FOR_NETWORK){
                    // HERE end service and notice user !!!
                    Log.d(TAG, "onStateChanged: Uploading '"+s3Key+"' to AWS-S3 bucket '"+bucketName+ "' (id:"+ id + " st:" + state.toString()+")");
                }
                if (TransferState.COMPLETED.equals(state)) {
                    Log.d(TAG, "Upload file '"+s3Key+"' to AWS-S3 bucket '"+bucketName+"' completed successfully.");
                    observer.cleanTransferListener();
                }
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentDone = (int)((float) bytesCurrent / (float) bytesTotal) * 100;
                Log.d(TAG, "Uploading file '"+s3Key+"' to AWS-S3 bucket '"+bucketName+"' progress: " + bytesCurrent + "/" + bytesTotal + " bytes");
                Log.d(TAG, "Uploading file '"+s3Key+"' to AWS-S3 bucket '"+bucketName+"' progress: " + percentDone + "%");
            }
            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "Error uploading file '"+s3Key+"' to AWS-S3 bucket '"+bucketName+"' :" + ex.getMessage());
            }
        });
    }

    public File downloadFile(ImageView imageView, AmazonS3 s3Client, String s3Key) {
        File file = createTempFile();
        TransferObserver observer = transferUtility.download(bucketName, s3Key, file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "onStateChanged: Downloading '"+s3Key+"' from AWS-S3 bucket '"+bucketName+ "' (id:"+ id + " st:" + state.toString()+")");
                // If upload error, failed or network disconnect
                if(state == TransferState.FAILED || state == TransferState.WAITING_FOR_NETWORK){
                    // HERE end service and notice user !!!
                    Log.d(TAG, "onStateChanged: Downloading '"+s3Key+"' from AWS-S3 bucket '"+bucketName+ "' (id:"+ id + " st:" + state.toString()+")");
                }
                if (TransferState.COMPLETED.equals(state)) {
                    Log.d(TAG, "Download file '"+s3Key+"' from AWS-S3 bucket '"+bucketName+"' completed successfully.");
                    imageView.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                    observer.cleanTransferListener();
                }
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentDone = (int)((float) bytesCurrent / (float) bytesTotal) * 100;
                Log.d(TAG, "Downloading file '"+s3Key+"' from AWS-S3 bucket '"+bucketName+"' progress: " + bytesCurrent + "/" + bytesTotal + " bytes");
                Log.d(TAG, "Downloading file '"+s3Key+"' from AWS-S3 bucket '"+bucketName+"' progress: " + percentDone + "%");
            }
            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "Error downloading file '"+s3Key+"' from AWS-S3 bucket '"+bucketName+"' :" + ex.getMessage());
            }
        });
        return file;
    }

    public void backupFile(AmazonS3 s3Client, String s3Key, String newS3Key) {
        // check if the object exists in the bucket
        boolean objectExists = s3Client.doesObjectExist(bucketName, s3Key);
        if (objectExists) {
            // backup the object if it exists
            try {
                // Copy the object with the new name
                CopyObjectResult result = s3Client.copyObject(bucketName, s3Key, bucketName, newS3Key);
                // Check if the copy operation was successful
                if (result != null) {
                    System.out.println("S3 file copied successfully.");
                } else {
                    System.out.println("S3 file copy failed.");
                }
                Log.d(TAG, "Object '" + s3Key + "' backed up to '" + newS3Key + "' in AWS-S3 bucket '" + bucketName + "' was succesful.");
            } catch (AmazonClientException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Object '" + s3Key + "' does not exist in AWS-S3 bucket '" + bucketName + "' and cannot be renamed.");
        }
    }

    public void renameFile(AmazonS3 s3Client, String oldS3Key, String newS3Key) {
        // check if the object exists in the bucket
        boolean objectExists = s3Client.doesObjectExist(bucketName, oldS3Key);
        if (objectExists) {
            // rename the object if it exists
            try {
                // Copy the object with the new name
                CopyObjectResult copyresult = s3Client.copyObject(bucketName, oldS3Key, bucketName, newS3Key);
                // Check if the copy operation was successful
                if (copyresult != null) {
                    System.out.println("S3 file copied successfully.");
                    // Delete the original file
                    deleteFile(s3Client, oldS3Key);
                } else {
                    System.out.println("S3 file copy failed.");
                }

                Log.d(TAG, "Object '" + oldS3Key + "' renamed to '" + newS3Key + "' in AWS-S3 bucket '" + bucketName + "'.");
            } catch (AmazonClientException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Object '" + oldS3Key + "' does not exist in AWS-S3 bucket '" + bucketName + "' and cannot be renamed.");
        }
    }

    public void deleteFile(AmazonS3 s3Client, String s3Key) throws AmazonClientException {
        // check if the object exists in the bucket
        boolean objectExists = s3Client.doesObjectExist(bucketName, s3Key);
        if (objectExists) {
            // delete the object if it exists
            try {
                DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucketName, s3Key);
                s3Client.deleteObject(deleteRequest);
                Log.d(TAG, "Object '"+ s3Key+ "' deleted from AWS-S3 bucket '"+ bucketName+ "'.");
            } catch (AmazonClientException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Object '"+ s3Key+ "' does not exist in AWS-S3 bucket '"+ bucketName+ "' and cannot be deleted.");
        }
    }

    public AmazonS3 getS3Client() {
        return (s3Client == null) ? new AmazonS3Client(new AnonymousAWSCredentials(), Region.getRegion(keys.getRegion())) : s3Client;
    }

    public void shutdown() {
        transferUtility.cancelAllWithType(TransferType.UPLOAD);
        transferUtility.cancelAllWithType(TransferType.DOWNLOAD);
        Log.d(TAG, "TransferUtility shut down");
    }

    private File createTempFile() {
        // create a temporary file
        File tempFile = null;
        try {
            tempFile = File.createTempFile("AWS_S3_", ".tmp");
            tempFile.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return(tempFile);
    }

    private File getFileFromResource(Context context, int resourceId) {
        // create a temporary file to copy the resource contents into
        File tempFile = createTempFile();
        // get an input stream for the resource file
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        // copy the contents of the resource into the temporary file
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            //The buffer size of 4096 bytes is a commonly used value because it is a multiple of the typical disk block size on most file systems,
            //which can improve the efficiency of reading and writing files. However, the buffer size can be adjusted based on the specific requirements of your use case.
            //Note that the size of the buffer can affect the performance of the file I/O operations.
            //Larger buffer sizes can improve performance by reducing the number of I/O operations required to read and write the file, but can also consume more memory.
            //Conversely, smaller buffer sizes can conserve memory, but may result in more I/O operations and slower performance. It is important to choose an appropriate buffer size based on the specific needs of your application.
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return(tempFile);
    }
    interface Callback {
        void onSuccess(Object result);
        void onError(Exception e);
    }
}