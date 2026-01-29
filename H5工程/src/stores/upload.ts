/**
 * 上传任务Store
 * 管理所有上传任务
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UploadTask, UploadTaskStatus } from '../types'
import { useConfigStore } from './config'

export const useUploadStore = defineStore('upload', () => {
  // 状态
  const tasks = ref<UploadTask[]>([])

  // ============================================
  // Getters
  // ============================================

  /**
   * 所有任务
   */
  const allTasks = computed(() => tasks.value)

  /**
   * 进行中的任务
   */
  const activeTasks = computed(() => 
    tasks.value.filter(task => task.status === 'uploading')
  )

  /**
   * 等待中的任务
   */
  const waitingTasks = computed(() => 
    tasks.value.filter(task => task.status === 'waiting')
  )

  /**
   * 失败的任务
   */
  const failedTasks = computed(() => 
    tasks.value.filter(task => task.status === 'failed')
  )

  /**
   * 成功的任务
   */
  const successTasks = computed(() => 
    tasks.value.filter(task => task.status === 'success')
  )

  // ============================================
  // Actions
  // ============================================

  /**
   * 添加任务
   */
  function addTask(task: UploadTask) {
    tasks.value.push(task)
  }

  /**
   * 更新任务
   */
  function updateTask(id: string, updates: Partial<UploadTask>) {
    const task = tasks.value.find(t => t.id === id)
    if (task) {
      Object.assign(task, updates)
    }
  }

  /**
   * 删除任务
   */
  function removeTask(id: string) {
    const index = tasks.value.findIndex(t => t.id === id)
    if (index > -1) {
      tasks.value.splice(index, 1)
    }
  }

  /**
   * 获取任务
   */
  function getTask(id: string): UploadTask | undefined {
    return tasks.value.find(t => t.id === id)
  }

  /**
   * 清空所有任务
   */
  function clearTasks() {
    tasks.value = []
  }

  /**
   * 清空已完成的任务
   */
  function clearCompletedTasks() {
    tasks.value = tasks.value.filter(
      task => task.status !== 'success' && task.status !== 'failed'
    )
  }

  return {
    // State
    tasks,
    // Getters
    allTasks,
    activeTasks,
    waitingTasks,
    failedTasks,
    successTasks,
    // Actions
    addTask,
    updateTask,
    removeTask,
    getTask,
    clearTasks,
    clearCompletedTasks
  }
})

