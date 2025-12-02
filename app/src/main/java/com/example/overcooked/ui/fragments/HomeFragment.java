package com.example.overcooked.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.OvercookedApplication;
import com.example.overcooked.R;
import com.example.overcooked.data.repository.ProjectRepository;
import com.example.overcooked.data.repository.TaskRepository;
import com.example.overcooked.ui.MainNavActivity;
import com.example.overcooked.ui.home.CookedMeterController;
import com.example.overcooked.ui.home.HomeStatsController;
import com.example.overcooked.ui.home.WorkNowController;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/**
 * Home Fragment - Main Dashboard
 * Shows Cooked Meter and Work Now tasks
 */
public class HomeFragment extends Fragment {

    // Views
    private TextView cookedLevelText;
    private TextView cookedPercentage;
    private TextView cookedStatusText;
    private TextView cookedContextText;
    private LinearProgressIndicator cookedProgressBar;
    private ImageView cookedIcon;
    private RecyclerView quickStatsRecycler;
    private RecyclerView workNowRecycler;
    private FloatingActionButton fabAddTask;
    private TextView viewAllTasksText;
    private View emptyStateLayout;
    private TextView coinScoreText;

    // Controllers
    private CookedMeterController cookedMeterController;
    private HomeStatsController homeStatsController;
    private WorkNowController workNowController;

    // Repositories
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private com.example.overcooked.data.repository.UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        taskRepository = ((OvercookedApplication) requireActivity().getApplication()).getTaskRepository();
        projectRepository = ((OvercookedApplication) requireActivity().getApplication()).getProjectRepository();
        userRepository = ((OvercookedApplication) requireActivity().getApplication()).getUserRepository();
        
        initializeViews(view);
        initializeControllers();
        setupClickListeners();
        observeData();
    }

    private void initializeViews(View view) {
        cookedLevelText = view.findViewById(R.id.cookedLevelText);
        cookedPercentage = view.findViewById(R.id.cookedPercentage);
        cookedStatusText = view.findViewById(R.id.cookedStatusText);
        cookedContextText = view.findViewById(R.id.cookedContextText);
        cookedProgressBar = view.findViewById(R.id.cookedProgressBar);
        cookedIcon = view.findViewById(R.id.cookedIcon);
        quickStatsRecycler = view.findViewById(R.id.quickStatsRecycler);
        workNowRecycler = view.findViewById(R.id.workNowRecycler);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        viewAllTasksText = view.findViewById(R.id.viewAllTasksText);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        coinScoreText = view.findViewById(R.id.coinScoreText);
        
        View coinScoreCard = view.findViewById(R.id.coinScoreCard);
        if (coinScoreCard != null) {
            coinScoreCard.setOnClickListener(v -> {
                startActivity(new android.content.Intent(requireContext(), com.example.overcooked.ui.ShopActivity.class));
            });
        }
    }

    private void initializeControllers() {
        cookedMeterController = new CookedMeterController(
                this,
                cookedLevelText,
                cookedPercentage,
                cookedStatusText,
                cookedContextText,
                cookedProgressBar,
                cookedIcon
        );
        homeStatsController = new HomeStatsController(this, quickStatsRecycler, coinScoreText);
        workNowController = new WorkNowController(this, taskRepository, workNowRecycler, emptyStateLayout, fabAddTask);
    }

    private void setupClickListeners() {
        viewAllTasksText.setOnClickListener(v -> {
            if (requireActivity() instanceof MainNavActivity) {
                ((MainNavActivity) requireActivity()).navigateToTasks();
            }
        });
    }

    private void observeData() {
        // Observe user data for coins
        userRepository.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && coinScoreText != null) {
                coinScoreText.setText(String.valueOf(user.getCoins()));
            }
        });

        // Observe tasks for cooked meter and work now list
        taskRepository.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            cookedMeterController.render(tasks);
            workNowController.submitTasks(tasks);
        });

        // Observe individual stats - store the value and update
        taskRepository.getPendingTaskCount().observe(getViewLifecycleOwner(), pending -> {
            if (pending != null) homeStatsController.setPendingCount(pending);
        });
        taskRepository.getCompletedTaskCount().observe(getViewLifecycleOwner(), completed -> {
            if (completed != null) homeStatsController.setCompletedCount(completed);
        });
        taskRepository.getOverdueTaskCount().observe(getViewLifecycleOwner(), overdue -> {
            if (overdue != null) homeStatsController.setOverdueCount(overdue);
        });
        projectRepository.getActiveProjectCount().observe(getViewLifecycleOwner(), projects -> {
            if (projects != null) homeStatsController.setProjectCount(projects);
        });
        
        // Also observe total projects for better tracking
        projectRepository.getAllProjects().observe(getViewLifecycleOwner(), projects -> {
            if (projects != null) homeStatsController.setTotalProjectCount(projects.size());
        });
    }
}
