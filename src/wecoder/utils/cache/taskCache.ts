export const globalCache = new Map<string, any>()

export type ModelMeta = {
	id?: string // 大模型返回值的唯一id
	modelId?: string // wecode大模型id
	taskMessage?: string // 第一次输入的文本
	mode?: string // 当前使用的模式
}

export function setCache(taskId: string | undefined, response: ModelMeta): void {
	if (taskId) {
		globalCache.set(taskId, response)
	}
}

export function getCache(taskId: string): ModelMeta | undefined {
	if (taskId) {
		return globalCache.get(taskId)
	}
	return undefined
}

export function clearCache(taskId: string): void {
	if (taskId) {
		globalCache.delete(taskId)
	}
}
