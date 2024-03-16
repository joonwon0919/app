package com.ljw.todolist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.ljw.todolist.LoginActivity
import com.ljw.todolist.R
import com.ljw.todolist.databinding.ActivityMainBinding
import com.ljw.todolist.databinding.ItemTodoBinding
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)

        // 할 일 목록을 불러오기
        val todoList = loadTodoList()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = TodoAdapter(
                todoList,
                onClickDelteIcon = {
                    viewModel.deleteTodo(it)
                },
                onClickItem = {
                    viewModel.toggleTodo(it)
                }
            )
        }

        binding.addButton.setOnClickListener {
            val todo = Todo(binding.editTextText.text.toString())
            viewModel.addTodo(todo)
            // 할 일 목록을 저장하기
            saveTodoList(viewModel.todoLiveData.value ?: emptyList())
        }

        viewModel.todoLiveData.observe(this, Observer {
            (binding.recyclerView.adapter as TodoAdapter).setData(it)
            // 변경된 할 일 목록을 저장하기
            saveTodoList(it)
        })

        mFirebaseAuth = FirebaseAuth.getInstance()

        val btnLogout = findViewById<Button>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            //로그아웃
            mFirebaseAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val btndel = findViewById<Button>(R.id.btn_det)
        btndel.setOnClickListener {
            //로그아웃
            val user = mFirebaseAuth.currentUser
            user?.delete()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 사용자 삭제가 성공한 경우
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // 사용자 삭제가 실패한 경우
                        Toast.makeText(this@MainActivity, "회원탈퇴 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveTodoList(todoList: List<Todo>) {
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(todoList)
        editor.putString("todo_list", json)
        editor.apply()
    }

    private fun loadTodoList(): List<Todo> {
        val json = sharedPreferences.getString("todo_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Todo>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    data class Todo(val text: String, var isDone: Boolean = false)

    class TodoAdapter(
        private var dataSet: List<Todo>,
        val onClickDelteIcon: (todo: Todo) -> Unit,
        val onClickItem: (todo: Todo) -> Unit
    ) :
        RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

        class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TodoViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_todo, viewGroup, false)

            return TodoViewHolder(ItemTodoBinding.bind(view))
        }

        override fun getItemCount(): Int {
            return dataSet.size
        }

        fun setData(newData: List<Todo>) {
            dataSet = newData
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(viewHolder: TodoViewHolder, position: Int) {
            val todo = dataSet[position]
            viewHolder.binding.todoText.text = todo.text
            viewHolder.binding.deleteImgView.setOnClickListener {
                onClickDelteIcon.invoke(todo)
            }

            viewHolder.binding.root.setOnClickListener {
                onClickItem.invoke(todo)
            }

            if (todo.isDone) {
                viewHolder.binding.todoText.apply {
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    setTypeface(null, Typeface.ITALIC)
                }
            } else {
                viewHolder.binding.todoText.apply {
                    paintFlags = 0
                    setTypeface(null, Typeface.NORMAL)
                }
            }
        }
    }

    class MainViewModel : ViewModel() {

        val todoLiveData = MutableLiveData<List<Todo>>()
        private val data = arrayListOf<Todo>()

        fun toggleTodo(todo: Todo) {
            todo.isDone = !todo.isDone
            todoLiveData.value = data
        }

        fun addTodo(todo: Todo) {
            data.add(todo)
            todoLiveData.value = data
        }

        fun deleteTodo(todo: Todo) {
            data.remove(todo)
            todoLiveData.value = data
        }
    }
}
