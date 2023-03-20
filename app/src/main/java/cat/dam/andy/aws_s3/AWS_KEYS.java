package cat.dam.andy.aws_s3;

import com.amazonaws.regions.Regions;

public class AWS_KEYS {
    // TODO: Change this to your own AWS keys
    // AWS CLI CREDENTIALS FOR LAB
    // You can get from menu LEARNER LAB / AWS DETAILS / AWS CLI / SHOW Button
    // Change all every time we stop and start the lab
    private static final String AWS_ACCESS_KEY="YOUR_ACCESS_KEY";//
    private static final String AWS_SECRET_ACCESS_KEY="YOUR_SECRET_ACCESS_KEY";//
    private static final String AWS_SESSION_TOKEN = "YOUR_SESSION_TOKEN";
    private static final String BUCKET_NAME = "YOUR BUCKET NAME";
    private static final Regions REGION = Regions.US_EAST_1;

    public String getAwsAccessKey() { return AWS_ACCESS_KEY;}
    public String getAwsSecretAccessKey() { return AWS_SECRET_ACCESS_KEY;}
    public String getAwsSessionToken() { return AWS_SESSION_TOKEN;}
    public Regions getRegion() { return REGION;}
    public String getBucketName() { return BUCKET_NAME;}
}
