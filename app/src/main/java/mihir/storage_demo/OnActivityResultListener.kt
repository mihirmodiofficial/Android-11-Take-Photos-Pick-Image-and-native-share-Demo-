package mihir.storage_demo

import androidx.activity.result.ActivityResult

interface OnActivityResultListener{
        fun onActivityResult(
            result: ActivityResult,
            currentRequestCode: Int
        )
    }