package com.mehran.imagetopdfconverter;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final static int IMAGE_PICK_REQ = 100;
    RecyclerView rvImages;
    ImageView ivPdfListActivity;
    Button btnAddImages, btnConvert;
    PicturesAdapter picturesAdapter;
    ArrayList<Picture> picturesList;
    CircularProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
        setUpSwipe();

        ivPdfListActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,PdfListActivity.class));
            }
        });

        btnAddImages.setOnClickListener(v -> {
            Intent imagePick = new Intent(Intent.ACTION_PICK);
            imagePick.setType("image/*");
            imagePick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(imagePick, "Select Images"), IMAGE_PICK_REQ);
        });

        // When convert button is clicked, show filename dialog and then create PDF via callback
        btnConvert.setOnClickListener(v -> {
            if (picturesList.isEmpty()) {
                Toast.makeText(this, "No images to convert", Toast.LENGTH_SHORT).show();
                return;
            }

            pickFileName(fileName -> {
                if (fileName == null) {
                    return; // cancelled
                }
                String finalName = fileName.trim().isEmpty() ? "images" : fileName.trim();
                new ConvertTask().execute(finalName);
            });
        });
    }

    public void init() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        picturesList = new ArrayList<>();
        btnConvert = findViewById(R.id.btnConvert);
        btnAddImages = findViewById(R.id.btnAddImages);
        ivPdfListActivity = findViewById(R.id.ivPDFIcon);
        rvImages = findViewById(R.id.rvImages);
        progressIndicator = findViewById(R.id.progressIndicator);

        picturesAdapter = new PicturesAdapter(this, picturesList);
        rvImages.setLayoutManager(new GridLayoutManager(this, 2));
        rvImages.setAdapter(picturesAdapter);

        btnConvert.setVisibility(View.INVISIBLE);
        updateUIForListState();
    }

    private void updateUIForListState() {
        boolean isEmpty = picturesList.isEmpty();
        rvImages.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        findViewById(R.id.emptyStateContainer).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        btnConvert.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);
        btnConvert.setEnabled(!isEmpty);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_REQ) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        picturesList.add(new Picture(uri));
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    picturesList.add(new Picture(uri));
                }
                picturesAdapter.notifyDataSetChanged();
                updateUIForListState();
            } else {
                Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setUpSwipe() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                int position = viewHolder.getAdapterPosition();
                if (direction == ItemTouchHelper.LEFT) {
                    picturesList.remove(position);
                    picturesAdapter.notifyItemRemoved(position);
                    updateUIForListState();
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(rvImages);
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    void openPdf(File outPutFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", outPutFile);

            Intent openFile = new Intent(Intent.ACTION_VIEW);
            openFile.setDataAndType(pdfUri, "application/pdf");
            openFile.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(openFile);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No PDF viewer app installed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Issue Loading : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void pickFileName(FileNameCallback callback) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.file_name_dialog_design,null);
        Dialog fileNameDialog = new Dialog(this);
        fileNameDialog.setContentView(dialogView);
        fileNameDialog.setCancelable(true);
        fileNameDialog.show();

        EditText etFileName = dialogView.findViewById(R.id.et_filename);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnSave.setOnClickListener(v -> {
            String fileName = etFileName.getText().toString().trim();
            if (TextUtils.isEmpty(fileName)){
                etFileName.setError("File name must not be Empty!!");
                return;
            }
            callback.onFileNamePicked(fileName);
            fileNameDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            fileNameDialog.dismiss();
            callback.onFileNamePicked(null); // user cancelled, send null
        });
    }

    public interface FileNameCallback {
        void onFileNamePicked(String fileName);
    }

    private class ConvertTask extends AsyncTask<String, Void, File> {
        private Exception exception;

        @Override
        protected void onPreExecute() {
            progressIndicator.setVisibility(View.VISIBLE);
            btnAddImages.setEnabled(false);
            btnConvert.setEnabled(false);
        }

        @Override
        protected File doInBackground(String... params) {
            String fileName = params[0];
            Document document = new Document();
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (storageDir == null) {
                exception = new Exception("Unable to access storage");
                return null;
            }

            File outPutFile = new File(storageDir, fileName + ".pdf");

            try {
                PdfWriter.getInstance(document, new FileOutputStream(outPutFile));
                document.open();

                boolean first = true;
                for (Picture pic : picturesList) {
                    Uri picUri = pic.getImageUri();
                    InputStream inputStream = getContentResolver().openInputStream(picUri);
                    if (inputStream == null) continue;
                    byte[] imageBytes = getBytes(inputStream);

                    Image image = Image.getInstance(imageBytes);

                    float pageWidth = document.getPageSize().getWidth()
                            - document.leftMargin()
                            - document.rightMargin();
                    float scaler = (pageWidth / image.getWidth()) * 100;
                    image.scalePercent(scaler);

                    // Put each image on its own page
                    if (!first) {
                        document.newPage();
                    } else {
                        first = false;
                    }

                    image.setAlignment(Element.ALIGN_CENTER | Element.ALIGN_TOP);
                    document.add(image);
                }

                document.close();

                return outPutFile;

            } catch (Exception e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(File result) {
            progressIndicator.setVisibility(View.GONE);
            btnAddImages.setEnabled(true);
            if (result != null) {
                Toast.makeText(MainActivity.this, "PDF Created Successfully: " + result.getAbsolutePath(), Toast.LENGTH_LONG).show();
                openPdf(result);
                picturesList.clear();
                picturesAdapter.notifyDataSetChanged();
                updateUIForListState();
            } else {
                String msg = "Issue: " + (exception != null ? exception.getMessage() : "Unknown");
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                updateUIForListState();
            }
        }
    }
}