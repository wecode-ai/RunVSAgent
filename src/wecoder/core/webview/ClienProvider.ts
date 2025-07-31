import { PINNED_API_NAME } from "../config/config"

interface PinnedApiConfigs {
	[key: string]: boolean
}

export function markPinnedApiConfigs(configs: any[], pinnedApiConfigs: PinnedApiConfigs = {}): PinnedApiConfigs {
	let enableUpdate = false
	if (Object.keys(pinnedApiConfigs).length === 0) {
		enableUpdate = true
	}
	for (const config of configs) {
		if (enableUpdate && PINNED_API_NAME.includes(config.name)) {
			pinnedApiConfigs[config.id] = true
		}
	}
	return pinnedApiConfigs
}
