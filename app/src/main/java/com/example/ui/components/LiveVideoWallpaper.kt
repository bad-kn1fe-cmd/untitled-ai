package com.example.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@Composable
fun LiveVideoWallpaperPlayer(
    videoUri: String,
    alpha: Float = 0.35f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanUri = remember(videoUri) { videoUri.trim() }

    if (cleanUri.isBlank()) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha.coerceIn(0.05f, 1.0f))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val frameLayout = FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val textureView = TextureView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }

                var mediaPlayer: MediaPlayer? = null

                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        try {
                            mediaPlayer?.release()
                            mediaPlayer = MediaPlayer().apply {
                                setSurface(Surface(surface))
                                setVolume(0f, 0f)
                                isLooping = true

                                val uriToSet = when {
                                    cleanUri.startsWith("file://") -> Uri.parse(cleanUri)
                                    cleanUri.startsWith("/") || File(cleanUri).exists() -> Uri.fromFile(File(cleanUri))
                                    else -> Uri.parse(cleanUri)
                                }
                                setDataSource(ctx, uriToSet)
                                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                prepareAsync()
                                setOnPreparedListener { mp ->
                                    mp.start()
                                }
                                setOnErrorListener { _, _, _ -> true }
                            }
                        } catch (_: Exception) {}
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                        } catch (_: Exception) {}
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }

                frameLayout.addView(textureView)
                frameLayout.tag = cleanUri
                frameLayout
            },
            update = { frameLayout ->
                val currentTag = frameLayout.tag as? String
                if (currentTag != cleanUri) {
                    frameLayout.tag = cleanUri
                    val oldTexture = frameLayout.getChildAt(0) as? TextureView
                    if (oldTexture != null) {
                        frameLayout.removeView(oldTexture)
                        val newTextureView = TextureView(frameLayout.context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                        var mediaPlayer: MediaPlayer? = null
                        newTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                try {
                                    mediaPlayer?.release()
                                    mediaPlayer = MediaPlayer().apply {
                                        setSurface(Surface(surface))
                                        setVolume(0f, 0f)
                                        isLooping = true

                                        val uriToSet = when {
                                            cleanUri.startsWith("file://") -> Uri.parse(cleanUri)
                                            cleanUri.startsWith("/") || File(cleanUri).exists() -> Uri.fromFile(File(cleanUri))
                                            else -> Uri.parse(cleanUri)
                                        }
                                        setDataSource(frameLayout.context, uriToSet)
                                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                        prepareAsync()
                                        setOnPreparedListener { mp ->
                                            mp.start()
                                        }
                                        setOnErrorListener { _, _, _ -> true }
                                    }
                                } catch (_: Exception) {}
                            }

                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                try {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                } catch (_: Exception) {}
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                        frameLayout.addView(newTextureView)
                    }
                }
            }
        )
    }
}
