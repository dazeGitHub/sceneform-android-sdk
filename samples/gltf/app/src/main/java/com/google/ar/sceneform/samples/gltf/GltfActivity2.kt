package com.google.ar.sceneform.samples.gltf

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.samples.common.helpers.CameraPermissionHelper
import com.google.ar.sceneform.samples.utils.ArCheckUtils
import com.google.ar.sceneform.samples.utils.ToastUtil
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_gltf2.*
import java.lang.ref.WeakReference

class GltfActivity2 : AppCompatActivity() {

    private var mArFragment: ArFragment? = null
    private var mRenderable: Renderable? = null
    private var mAnchor: AnchorNode? = null

    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }

        setContentView(R.layout.activity_gltf2)
        mArFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        renderScene()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions!!, results!!)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            ToastUtil.showShortToast("Camera permission is needed to run this application")
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }else{
            setContentView(R.layout.activity_gltf2)
            mArFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
            renderScene()
        }
    }

    private fun renderScene() {
        val result: Boolean = ArCheckUtils.checkDeviceSupportAr(this)
        if (result) {
            loadScene()
            initListener()
            ToastUtil.showShortToast("请先点击平面白圈用于添加一个锚点")
        }
    }

    private fun initListener() {
        mArFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
            if (mRenderable == null) {
                return@setOnTapArPlaneListener
            }
            if (mAnchor == null) {
                mAnchor = addAnchor(hitResult)
            }
        }

        btn_alter_plane.setOnClickListener {
            alterPlane()
        }

        btn_add_node.setOnClickListener {
            addTigerNode()
        }

        btn_add_light.setOnClickListener {
            addLight()
        }
    }

    /**
     * 添加一个锚点，之后所有节点用这一个锚点
     */
    private fun addAnchor(hitResult: HitResult): AnchorNode {
        //父子关系: child -> Parent
        // tigerTitleNode -> TransformableNode 的 model -> anchorNode -> arFragment.getArSceneView().getScene()
        // Create the Anchor.
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(mArFragment!!.arSceneView.scene)

        ToastUtil.showShortToast("添加锚点成功")

        return anchorNode
    }

    private fun addTigerNode(): Node? {

        if (mAnchor == null) {
            ToastUtil.showShortToast("锚点为空，请点击平面添加锚点")
            return null
        }

        // Create the transformable model and add it to the anchor.
        val model = TransformableNode(mArFragment!!.transformationSystem)
        model.setParent(mAnchor)
        model.scaleController.maxScale = 0.2f
        model.scaleController.minScale = 0.1f
        model.renderable = mRenderable
        model.select()

        val tigerTitleNode = Node()
        tigerTitleNode.setParent(model)
        tigerTitleNode.isEnabled = false
        tigerTitleNode.localPosition = Vector3(0.0f, 1.0f, 0.0f)

//        tigerTitleNode.renderable = mRenderable
//        tigerTitleNode.isEnabled = true

        ToastUtil.showShortToast("添加了一个老虎节点")

        return tigerTitleNode
    }

    private fun addLight() {
        if (mAnchor == null) {
            ToastUtil.showShortToast("锚点为空，请点击平面添加锚点")
            return
        }

        //添加一个聚光灯
        val myLight = Light.builder(Light.Type.DIRECTIONAL)
                .setColor(Color(0xffff00))
                .setShadowCastingEnabled(true)
                .build()

        ToastUtil.showShortToast("为锚点添加了一个灯光")

        mAnchor?.light = myLight
    }

    private fun alterPlane() {
        //修改平面
        val sampler: Texture.Sampler = Texture.Sampler.builder()
                .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
                .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
                .build()

        Texture.builder()
                .setSource(this, R.drawable.custom_texture)
                .setSampler(sampler)
                .build()
                .thenAccept { texture ->
                    mArFragment!!.arSceneView.getPlaneRenderer()
                            .material
                            .get()
                            .setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture)
                }.exceptionally { throwable: Throwable? ->
                    ToastUtil.showShortToast("set Texture failed" + throwable?.message)
                    Log.e(TAG, "set Texture failed" + throwable?.message)
                    null
                }
    }

    private fun loadScene() {
        val weakActivity = WeakReference(this)

        //glb 文件在线查看: https://techbrood.com/tool?p=gltf-viewer，根据如下的 glb 链接下载 glb 文件后上传到该网站查看
        ModelRenderable.builder()
                .setSource(this, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept { modelRenderable: ModelRenderable? ->
                    val activity = weakActivity.get()
                    if (activity != null) {
                        activity.mRenderable = modelRenderable
                    }
                }
                .exceptionally { throwable: Throwable? ->
                    ToastUtil.showShortToast("Unable to load Tiger renderable")
                    null
                }
    }

    companion object {
        private val TAG = GltfActivity::class.java.simpleName
        private const val MIN_OPENGL_VERSION = 3.0
    }
}
