package com.student.overcooked.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.student.overcooked.data.LocalCoinStore;
import com.student.overcooked.data.repository.UserRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Pushes pending local coin deltas to backend when connectivity returns.
 */
public class SyncWorker extends Worker {
    private static final String UNIQUE_SYNC = "overcooked_sync";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        LocalCoinStore store = new LocalCoinStore(getApplicationContext());
        int pending = store.getPendingDelta();
        if (pending == 0) {
            return Result.success();
        }

        UserRepository userRepository = new UserRepository();
        CountDownLatch latch = new CountDownLatch(1);
        final Result[] resultHolder = new Result[]{Result.retry()};

        userRepository.updateCoinsBy(pending,
                newBalance -> {
                    store.setBalanceFromServer(newBalance);
                    resultHolder[0] = Result.success();
                    latch.countDown();
                },
                e -> {
                    // Keep pending delta for retry
                    resultHolder[0] = Result.retry();
                    latch.countDown();
                });

        try {
            if (!latch.await(12, TimeUnit.SECONDS)) {
                return Result.retry();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }

        return resultHolder[0];
    }

    public static void enqueue(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_SYNC, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }
}
