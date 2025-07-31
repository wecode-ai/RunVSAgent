export interface UserAction {
	url: string
	value: string
}

export const UserActionEnum: { [key: string]: UserAction } = {
	wecoder_save: { url: "/v1/chat/action", value: "wecoder_save" },
	wecoder_switch_model: { url: "/v1/chat/action", value: "wecoder_switch_model" },
}
