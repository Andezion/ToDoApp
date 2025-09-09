package com.example.todo.ui.main;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todo.R;
import com.example.todo.data.database.entities.Task;
import com.example.todo.utils.DateUtils;

/**
 * Адаптер для отображения списка задач в RecyclerView
 * Путь: app/src/main/java/com/yourpackage/todoapp/ui/main/TaskAdapter.java
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private OnTaskClickListener listener;

    public TaskAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // DiffUtil для эффективного обновления списка
    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getDescription().equals(newItem.getDescription()) &&
                    oldItem.isCompleted() == newItem.isCompleted() &&
                    oldItem.getCompletionTime() == newItem.getCompletionTime() &&
                    oldItem.getCategory().equals(newItem.getCategory()) &&
                    oldItem.isHasAttachments() == newItem.isHasAttachments() &&
                    oldItem.isNotificationEnabled() == newItem.isNotificationEnabled();
        }
    };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = getItem(position);
        holder.bind(currentTask);
    }

    // ViewHolder класс
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvDescription;
        private TextView tvDueDate;
        private TextView tvCategory;
        private CheckBox cbCompleted;
        private ImageView ivAttachment;
        private ImageView ivNotification;
        private ImageView ivPriority;
        private View vCategoryIndicator;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            ivAttachment = itemView.findViewById(R.id.ivAttachment);
            ivNotification = itemView.findViewById(R.id.ivNotification);
            ivPriority = itemView.findViewById(R.id.ivPriority);
            vCategoryIndicator = itemView.findViewById(R.id.vCategoryIndicator);

            // Обработчики кликов
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskClick(getItem(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskLongClick(getItem(position));
                    return true;
                }
                return false;
            });

            cbCompleted.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskCheckboxClick(getItem(position));
                }
            });
        }

        public void bind(Task task) {
            // Основная информация
            tvTitle.setText(task.getTitle());

            // Описание (показывать только если есть)
            if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
                tvDescription.setText(task.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Дата выполнения
            if (task.getCompletionTime() > 0) {
                tvDueDate.setText(DateUtils.getRelativeTimeString(task.getCompletionTime()));
                tvDueDate.setVisibility(View.VISIBLE);

                // Цвет даты в зависимости от срочности
                if (DateUtils.isOverdue(task.getCompletionTime()) && !task.isCompleted()) {
                    tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.error));
                } else if (DateUtils.getDaysUntilDue(task.getCompletionTime()) <= 1 && !task.isCompleted()) {
                    tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.warning));
                } else {
                    tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.on_surface_variant));
                }
            } else {
                tvDueDate.setVisibility(View.GONE);
            }

            // Категория
            tvCategory.setText(task.getCategory());

            // Индикатор категории (цветная полоска)
            setCategoryIndicatorColor(task.getCategory());

            // Checkbox состояния
            cbCompleted.setChecked(task.isCompleted());

            // Стиль для завершенных задач
            if (task.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvDescription.setPaintFlags(tvDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                itemView.setAlpha(0.6f);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvDescription.setPaintFlags(tvDescription.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                itemView.setAlpha(1.0f);
            }

            // Иконки
            ivAttachment.setVisibility(task.isHasAttachments() ? View.VISIBLE : View.GONE);
            ivNotification.setVisibility(task.isNotificationEnabled() ? View.VISIBLE : View.GONE);

            // Приоритет (срочность)
            if (DateUtils.isOverdue(task.getCompletionTime()) && !task.isCompleted()) {
                ivPriority.setVisibility(View.VISIBLE);
                ivPriority.setImageResource(R.drawable.ic_priority_high);
                ivPriority.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.error));
            } else if (DateUtils.getDaysUntilDue(task.getCompletionTime()) <= 1 && !task.isCompleted()) {
                ivPriority.setVisibility(View.VISIBLE);
                ivPriority.setImageResource(R.drawable.ic_priority_medium);
                ivPriority.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.warning));
            } else {
                ivPriority.setVisibility(View.GONE);
            }
        }

        private void setCategoryIndicatorColor(String category) {
            int color;

            switch (category.toLowerCase()) {
                case "работа":
                    color = ContextCompat.getColor(itemView.getContext(), R.color.category_work);
                    break;
                case "личное":
                    color = ContextCompat.getColor(itemView.getContext(), R.color.category_personal);
                    break;
                case "учеба":
                    color = ContextCompat.getColor(itemView.getContext(), R.color.category_study);
                    break;
                case "покупки":
                    color = ContextCompat.getColor(itemView.getContext(), R.color.category_shopping);
                    break;
                case "здоровье":
                    color = ContextCompat.getColor(itemView.getContext(), R.color.category_health);
                    break;
                default:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.category_default);
                    break;
            }

            vCategoryIndicator.setBackgroundColor(color);
        }
    }

    // Интерфейс для обработки кликов
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task);
        void onTaskCheckboxClick(Task task);
    }
}