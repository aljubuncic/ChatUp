package ba.etf.chatapp.notifications

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface APIService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAALhoTykg:APA91bE3E8jTTJSb8zokBsNg-9mHP9qCnWr3rkeHFO5D1PVrHa64CK3wEElAADwZZymzdhwz8OAQa5lIVmxunZ9kFkYWO8fQwme0rAac9IwkvortZXToahq6heqvIc7kBwl0IEJNEYB5"
    )

    @POST("fcm/send")
    fun sendNotification(@Body body: Sender): Call<Response>
}