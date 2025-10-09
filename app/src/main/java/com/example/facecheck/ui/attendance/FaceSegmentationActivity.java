package com.example.facecheck.ui.attendance;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.facecheck.R;
import com.example.facecheck.adapter.FaceSegmentationAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 人脸分割展示界面
 * 显示检测到的人脸分割照片
 */
public class FaceSegmentationActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private TextView tvFaceCount;
    private Button btnBack, btnSave, btnDelete;
    private FaceSegmentationAdapter adapter;
    
    private List<String> faceImagePaths;
    private int faceCount;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_segmentation);
        
        // 获取传递的数据
        faceImagePaths = getIntent().getStringArrayListExtra("face_image_paths");
        faceCount = getIntent().getIntExtra("face_count", 0);
        
        if (faceImagePaths == null) {
            faceImagePaths = new ArrayList<>();
        }
        
        initViews();
        setupRecyclerView();
        displayFaceInfo();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvFaceCount = findViewById(R.id.tvFaceCount);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveSelectedFaces());
        btnDelete.setOnClickListener(v -> deleteSelectedFaces());
    }
    
    private void setupRecyclerView() {
        adapter = new FaceSegmentationAdapter(this, faceImagePaths);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 每行两列
        recyclerView.setAdapter(adapter);
        
        // 设置点击事件
        adapter.setOnItemClickListener(new FaceSegmentationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                showFaceDetail(position);
            }
            
            @Override
            public void onItemLongClick(int position) {
                // 长按选择/取消选择
                adapter.toggleSelection(position);
            }
        });
    }
    
    private void displayFaceInfo() {
        tvFaceCount.setText("检测到 " + faceCount + " 个人脸");
    }
    
    private void showFaceDetail(int position) {
        if (position >= 0 && position < faceImagePaths.size()) {
            String imagePath = faceImagePaths.get(position);
            File imageFile = new File(imagePath);
            
            if (imageFile.exists()) {
                // 创建对话框显示大图
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("人脸 " + (position + 1));
                
                ImageView imageView = new ImageView(this);
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                
                builder.setView(imageView);
                builder.setPositiveButton("确定", null);
                builder.setNegativeButton("删除", (dialog, which) -> {
                    deleteFaceImage(position);
                });
                
                android.app.AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void deleteFaceImage(int position) {
        if (position >= 0 && position < faceImagePaths.size()) {
            String imagePath = faceImagePaths.get(position);
            File imageFile = new File(imagePath);
            
            if (imageFile.exists() && imageFile.delete()) {
                faceImagePaths.remove(position);
                adapter.notifyItemRemoved(position);
                Toast.makeText(this, "已删除人脸图片", Toast.LENGTH_SHORT).show();
                
                // 更新人脸数量显示
                faceCount--;
                displayFaceInfo();
            } else {
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void saveSelectedFaces() {
        List<Integer> selectedPositions = adapter.getSelectedPositions();
        if (selectedPositions.isEmpty()) {
            Toast.makeText(this, "请先选择要保存的人脸", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "已保存 " + selectedPositions.size() + " 个人脸", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void deleteSelectedFaces() {
        List<Integer> selectedPositions = adapter.getSelectedPositions();
        if (selectedPositions.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的人脸", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 按降序删除，避免索引问题
        List<Integer> positionsToDelete = new ArrayList<>(selectedPositions);
        positionsToDelete.sort((a, b) -> b - a);
        
        for (int position : positionsToDelete) {
            deleteFaceImage(position);
        }
        
        // 清除选择状态
        adapter.clearSelection();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}