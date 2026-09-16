package com.review.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class ReviewTask(var id:String=UUID.randomUUID().toString(), var title:String, var answer:String="", var due:Long=System.currentTimeMillis(), var interval:Int=1, var ease:Double=2.5, var reps:Int=0, var difficulty:Int=2, var folder:String="عام", var deleted:Boolean=false, var selfDev:Boolean=false, var created:Long=System.currentTimeMillis(), var lastReview:Long=0)
data class Goal(var id:String=UUID.randomUUID().toString(), var title:String, var message:String="", var start:Long=System.currentTimeMillis(), var end:Long=System.currentTimeMillis()+30L*86400000, var completed:Boolean=false)
data class Folder(var id:String=UUID.randomUUID().toString(), var name:String, var icon:String="📚")
data class ChatMessage(val role:String,val text:String,val time:Long=System.currentTimeMillis())

class ReviewRepository(c:Context){
 private val p=c.getSharedPreferences("review_native",Context.MODE_PRIVATE)
 var tasks:MutableList<ReviewTask> = loadTasks(); private set
 var goals:MutableList<Goal> = loadGoals(); private set
 var folders:MutableList<Folder> = loadFolders(); private set
 var chats:MutableList<ChatMessage> = loadChats(); private set
 var xp:Int = p.getInt("xp",0); private set
 var streak:Int = p.getInt("streak",0); private set
 fun save(){ p.edit().putInt("xp",xp).putInt("streak",streak).putString("tasks",JSONArray().apply{tasks.forEach{put(taskJson(it))}}.toString()).putString("goals",JSONArray().apply{goals.forEach{put(goalJson(it))}}.toString()).putString("folders",JSONArray().apply{folders.forEach{put(folderJson(it))}}.toString()).putString("chats",JSONArray().apply{chats.forEach{put(JSONObject().put("r",it.role).put("t",it.text).put("time",it.time))}}.toString()).apply() }
 fun addTask(t:ReviewTask){tasks.add(t);save()}; fun updateTask(t:ReviewTask){val i=tasks.indexOfFirst{it.id==t.id};if(i>=0)tasks[i]=t;save()}; fun deleteTask(id:String){tasks.find{it.id==id}?.deleted=true;save()}; fun restore(id:String){tasks.find{it.id==id}?.deleted=false;save()}; fun purge(id:String){tasks.removeAll{it.id==id};save()}
 fun review(t:ReviewTask,rating:Int){ val now=System.currentTimeMillis(); t.lastReview=now;t.reps++;t.difficulty=when(rating){1->3;2->2;3->2;else->1}; val days=when(rating){1->1;2->maxOf(1,t.interval);3->maxOf(1,(t.interval*2));else->maxOf(2,(t.interval*t.ease).toInt())};t.interval=days;t.ease=(t.ease + when(rating){1->-0.2;2->-0.05;3->0.1;else->0.2}).coerceIn(1.3,3.2);t.due=now+days*86400000L;xp+=when(rating){1->5;2->10;3->15;else->20};save()}
 fun addGoal(g:Goal){goals.add(g);save()}; fun toggleGoal(id:String){goals.find{it.id==id}?.let{it.completed=!it.completed};save()}; fun deleteGoal(id:String){goals.removeAll{it.id==id};save()}
 fun addFolder(f:Folder){if(folders.none{it.name==f.name})folders.add(f);save()}; fun deleteFolder(id:String){folders.removeAll{it.id==id};save()}
 fun addChat(m:ChatMessage){chats.add(m);save()}; fun clearChats(){chats.clear();save()}
 fun backup():String=JSONObject().put("version",2).put("xp",xp).put("streak",streak).put("tasks",JSONArray().apply{tasks.forEach{put(taskJson(it))}}).put("goals",JSONArray().apply{goals.forEach{put(goalJson(it))}}).put("folders",JSONArray().apply{folders.forEach{put(folderJson(it))}}).toString(2)
 fun restoreJson(raw:String){val o=JSONObject(raw);tasks=mutableListOf<ReviewTask>().also{a->o.optJSONArray("tasks")?.let{j->for(i in 0 until j.length())a.add(parseTask(j.getJSONObject(i)))}};goals=mutableListOf<Goal>().also{a->o.optJSONArray("goals")?.let{j->for(i in 0 until j.length())a.add(parseGoal(j.getJSONObject(i)))}};folders=mutableListOf<Folder>().also{a->o.optJSONArray("folders")?.let{j->for(i in 0 until j.length())a.add(Folder(j.getJSONObject(i).optString("id",UUID.randomUUID().toString()),j.getJSONObject(i).optString("name"),j.getJSONObject(i).optString("icon","📚")))}};save()}
 private fun taskJson(t:ReviewTask)=JSONObject().apply{put("id",t.id);put("title",t.title);put("answer",t.answer);put("due",t.due);put("interval",t.interval);put("ease",t.ease);put("reps",t.reps);put("difficulty",t.difficulty);put("folder",t.folder);put("deleted",t.deleted);put("selfDev",t.selfDev);put("created",t.created);put("lastReview",t.lastReview)}
 private fun goalJson(g:Goal)=JSONObject().apply{put("id",g.id);put("title",g.title);put("message",g.message);put("start",g.start);put("end",g.end);put("completed",g.completed)}
 private fun folderJson(f:Folder)=JSONObject().put("id",f.id).put("name",f.name).put("icon",f.icon)
 private fun parseTask(o:JSONObject)=ReviewTask(o.optString("id",UUID.randomUUID().toString()),o.optString("title"),o.optString("answer"),o.optLong("due",System.currentTimeMillis()),o.optInt("interval",1),o.optDouble("ease",2.5),o.optInt("reps"),o.optInt("difficulty",2),o.optString("folder","عام"),o.optBoolean("deleted"),o.optBoolean("selfDev"),o.optLong("created"),o.optLong("lastReview"))
 private fun parseGoal(o:JSONObject)=Goal(o.optString("id",UUID.randomUUID().toString()),o.optString("title"),o.optString("message"),o.optLong("start"),o.optLong("end"),o.optBoolean("completed"))
 private fun loadTasks()=mutableListOf<ReviewTask>().also{a->val j=JSONArray(p.getString("tasks","[]"));for(i in 0 until j.length())a.add(parseTask(j.getJSONObject(i)))}
 private fun loadGoals()=mutableListOf<Goal>().also{a->val j=JSONArray(p.getString("goals","[]"));for(i in 0 until j.length())a.add(parseGoal(j.getJSONObject(i)))}
 private fun loadFolders()=mutableListOf<Folder>().also{a->val j=JSONArray(p.getString("folders", "[{\"id\":\"default\",\"name\":\"عام\",\"icon\":\"📚\"}]"));for(i in 0 until j.length()){val o=j.getJSONObject(i);a.add(Folder(o.optString("id"),o.optString("name"),o.optString("icon","📚")))}}
 private fun loadChats()=mutableListOf<ChatMessage>().also{a->val j=JSONArray(p.getString("chats","[]"));for(i in 0 until j.length()){val o=j.getJSONObject(i);a.add(ChatMessage(o.optString("r"),o.optString("t"),o.optLong("time")))}}
}
fun fmtDate(ms:Long):String=SimpleDateFormat("yyyy/MM/dd",Locale("ar")).format(Date(ms))
fun isDue(t:ReviewTask):Boolean=t.due<=System.currentTimeMillis() && !t.deleted
