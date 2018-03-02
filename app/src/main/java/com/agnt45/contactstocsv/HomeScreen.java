package com.agnt45.contactstocsv;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.zip.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class HomeScreen extends AppCompatActivity {
    private Button exportContacts;
        private boolean statuscsv ;
        private Cursor cursor = null;
    private CoordinatorLayout coordinatorLayout;
    private File ZipDir;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        exportContacts = findViewById(R.id.Export);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        exportContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Observable.fromCallable(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return createCSV();
                    }
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> Snackbar.make(coordinatorLayout,
                                "Contacts Exported to"+ZipDir.toString(),Snackbar.LENGTH_LONG).show());

            }
        });

    }



    private Boolean  createCSV() throws FileNotFoundException {
        ZipDir = new File(Environment.getExternalStorageDirectory().
                getAbsolutePath()+"/my_test_contact");
        if(!ZipDir.isDirectory()){
            ZipDir.mkdirs();
        }

        CSVWriter csvWriter = null;
        try{
            csvWriter = new CSVWriter(new FileWriter(ZipDir +"/my_test_contact.csv"));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        String dName,number;
        long _id;
        String columns[] = new String[]{ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME};
        csvWriter.writeColumnNames();
        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                columns,null,null,
                ContactsContract.Data.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
        startManagingCursor(cursor);
        if (cursor.moveToFirst()){

            do{
                _id = Long.parseLong(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)));
                dName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)).trim();

                number = getPrimaryNumber(_id);
                csvWriter.writeNext((dName + "/" + number).split("/"));
            }while (cursor.moveToNext());
            statuscsv = true;
        }else{
            statuscsv = false;
        }
        try{
            csvWriter.close();
        }catch (IOException e){
            Log.w("WRITER",e.toString());
        }
        convertToZip(ZipDir);
        return statuscsv;
    }

    private String getPrimaryNumber(long id) {
        String primaryNumber = null;
        try {
            Cursor cursor = getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ id,
                    null,
                    null);
            if(cursor != null) {
                while(cursor.moveToNext()){
                    switch(cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))){
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE :
                            primaryNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME :
                            primaryNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK :
                            primaryNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER :
                    }
                    if(primaryNumber != null)
                        break;
                }
            }
        } catch (Exception e) {
            Log.i("PhoneNumber:", "Exception " + e.toString());
        } finally {
            if(cursor != null) {
                cursor.deactivate();
                cursor.close();
            }
        }
        return primaryNumber;
    }


    public void convertToZip(File file) throws FileNotFoundException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(
                new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath()+"/my_test_contact.zip"));
        try{
            File[] DirFiles = file.listFiles();
            for(int i=0;i < DirFiles.length;i++){
                File file1 = DirFiles[i];
                ZipEntry zipEntry = new ZipEntry(file1.getName());
                zipEntry.setSize(file1.length());
                zipEntry.setTime(file1.lastModified());
                zipOutputStream.putNextEntry(zipEntry);
                FileInputStream fileInputStream = new FileInputStream(file1);
                try {
                    IOUtils.copy(fileInputStream, zipOutputStream);
                } finally {
                    IOUtils.closeQuietly(fileInputStream);
                }
                zipOutputStream.closeEntry();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }


    }


}
