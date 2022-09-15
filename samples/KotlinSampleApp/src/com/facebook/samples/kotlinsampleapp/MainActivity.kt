package com.facebook.samples.kotlinsampleapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.facebook.samples.kotlinsampleapp.home.HomeViewModel

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val homeViewModel: HomeViewModel by viewModels()
    setContent { App(homeViewModel) }
  }
}
