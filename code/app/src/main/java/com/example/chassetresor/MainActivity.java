package com.example.chassetresor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private LocationScene locationScene;
    private ModelRenderable redCubeRenderable, andyRenderable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        ArSceneView arSceneView = arFragment.getArSceneView();

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material-> {
                            redCubeRenderable =
                                    ShapeFactory.makeCube(new Vector3(5f,5f,5f), new Vector3(0.0f,0.0f,0.0f), material);});

        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build();

        CompletableFuture.allOf(andy)
                .handle(
                        (notUsed, throwable) ->
                        {
                            if (throwable != null) {
                                return null;
                            }

                            try {
                                andyRenderable = andy.get();
                            } catch (InterruptedException | ExecutionException ex) {
                            }
                            return null;
                        }
                );

        /*
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (redCubeRenderable == null) {
                        return;
                    }
                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                        return;
                    }


                    //On créé le point d'encrage du modèle 3d
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    //On attache ensuite notre modèle au point d'encrage
                    TransformableNode Node = new TransformableNode(arFragment.getTransformationSystem());
                    Node.setParent(anchorNode);
                    Node.setRenderable(redCubeRenderable);
                    Node.select();
                }
        );

         */

        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {

                            if (locationScene == null) {
                                locationScene = new LocationScene(this, arSceneView);
                                locationScene.mLocationMarkers.add(
                                        new LocationMarker(
                                                4.918005,
                                                45.004795,
                                                getHint()));
                            }

                            if (locationScene != null) {
                                locationScene.processFrame(arSceneView.getArFrame());
                            }
                        });

    }

    private Node getHint() {
        Node hint = new Node();
        hint.setRenderable(andyRenderable);
        Context c = this;
        hint.setOnTapListener((v, event) -> {
            Toast.makeText(
                    c, "Hint touched.", Toast.LENGTH_LONG)
                    .show();
        });
        return hint;
    }
}