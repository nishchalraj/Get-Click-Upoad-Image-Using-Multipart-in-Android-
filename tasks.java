public class Tasks extends AppCompatActivity {

    //request codes for the tasks
    private static final int REQUEST_CHOSEN_IMAGE_CAPTURE = 3;
    private static final int RESULT_CHOSEN_IMAGE = 2;

    //a button that will pop up a dialog displaying the options to choose any image from
    Button mChooseImageButton;

    //button to make the upload process ongoing
    Button mPostButton;

    //in case there are more number of images and we may need arraylist to store the paths to al
    private ArrayList<String> imagePaths;
    //a display name that will be set on the text field
    private String displayName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        mChooseImageButton = findViewById(R.id.event_add_choose_file);

        mPostButton = findViewById(R.id.post_button);
        imagePaths = new ArrayList<>();

        mChooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

    		//very important step for the devices with API>19, automatic permissions are not given, so we need to make the user choose, so that he can allow for the permission
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Tasks.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 10);
                }

    		//or if the permission is given then the task will start		
		 else {
                    final CharSequence[] items = {"Take Photo", "Choose from Gallery"};
	    		//a dialog that will show the options to choose from
                    AlertDialog.Builder builder = new AlertDialog.Builder(Tasks.this);
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {

                            if (items[item].equals("Take Photo")) {
			    //make the app say that capturing action will be carried out  
                                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			       
				//checks if the camera permission is allowed or not
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                                {
                                    ActivityCompat.requestPermissions(Tasks.this,
                                            new String[]{Manifest.permission.CAMERA}, 10);
                                }

                                else
                                    startActivityForResult(takePictureIntent, REQUEST_CHOSEN_IMAGE_CAPTURE);

                            }
			         //image type of file should be chosen and automatically opens gallery
				 else if (items[item].equals("Choose from Gallery")) {

                                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                                i.setType("image/*");
                                startActivityForResult(i, RESULT_CHOSEN_IMAGE);

                            } else if (items[item].equals("Cancel")) {
                                dialog.dismiss();
                            }
                        }
                    });
                    builder.show();
                }
            }
        });

        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!imagePaths.isEmpty()){
                    sendPostRequestUsingMultipart(imagePaths);
                }
                else{
                    Toast.makeText(getApplicationContext(),"Select Image !", Toast.LENGTH_SHORT).show();
                }
            }
        });

    private void sendPostRequestUsingMultipart(String coverImagePath, List<String> imagePath) {

    	//checks availability of network
        if (!NetworkUtil.isNetworkAvailable()) {
            Toast.makeText(this, "No internet", Toast.LENGTH_SHORT);
            return;
        }

        try {
            final String uploadId = UUID.randomUUID().toString();

    	    //this is how Multipart request is defined
            final MultipartUploadRequest request = new MultipartUploadRequest(this, uploadId, /*ServerConstants*/);
            request.addFileToUpload(imagePath.get(0),"image") //can add more files to upload i.e. texts, pdfs, videos, e.t.c.
                    .setNotificationConfig(new UploadNotificationConfig()) //shows the uploading process on the notifiation panel
                    .setMaxRetries(2) //number of tries for a given upload action
                    .setDelegate(new UploadStatusDelegate() {

                        @Override
                        public void onProgress(Context context, UploadInfo uploadInfo) {}

                        @Override
                        public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {}

                        @Override
                        public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {}

                        @Override
                        public void onCancelled(Context context, UploadInfo uploadInfo) {}
                    });
	
    	    //add headers for the request and at the end start the upload process
            request.addHeader()
                    .addHeader()
                    .startUpload();

        } catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RESULT_CHOSEN_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData(); //uri of & from the chosen image data

 	    String selectedImagePath = FileUtils.getPath(Tasks.this, uri); //gives path of the uri

            File myFile = new File(uri.toString()); //make instance of File
            String path = myFile.getAbsolutePath();
            displayName = null;
            if (uri.toString().startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = getApplicationContext().getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else if (uri.toString().startsWith("file://")) {
                displayName = myFile.getName();
            }
            addPathView(selectedImagePath);
            imagePaths.add(selectedImagePath);
        }

        //for captured image from choose button
        if (requestCode == REQUEST_CHOSEN_IMAGE_CAPTURE && resultCode == RESULT_OK /* && data!=null && data.getData() != null */){

            Bitmap photo = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");

            // Call this method to get the uri from the bitmap
            Uri tempUri = getImageUri(Tasks.this, photo);

            String capturedImagePath = FileUtils.getPath(Tasks.this, tempUri);
            File myFile = new File(tempUri.toString());
            String path = myFile.getAbsolutePath();
            displayName = null;
            if (tempUri.toString().startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = getApplicationContext().getContentResolver().query(tempUri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else if (tempUri.toString().startsWith("file://")) {
                displayName = myFile.getName();
            }
            addPathView(capturedImagePath);
            imagePaths.add(capturedImagePath);
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}



