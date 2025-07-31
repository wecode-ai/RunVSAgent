import React, { useState, useEffect } from "react"
import { QRCodeSVG } from "qrcode.react"
import { vscode } from "@src/utils/vscode"
import { WebviewMessage } from "@roo/WebviewMessage"

const qrcodeExpireTime = 120 //查询状态超时时间

type LoginPageProps = {
	onDone: () => void
}

const LoginPage: React.FC<LoginPageProps> = ({ onDone }) => {
	const [qrcodeUrl, setQrcodeUrl] = useState("") // 使用 state 管理 qrcodeUrl
	const [currentQrcoeSid, setCurrentQrcoeSid] = useState("") // 使用 state 管理 currentQrcoeSid
	const [_remainingTime, setRemainingTime] = useState(qrcodeExpireTime)

	// 监听来自扩展的消息
	useEffect(() => {
		const messageHandler = (event: MessageEvent) => {
			const message = event.data
			if (message.type === "qrcodeInfo" && message.text) {
				try {
					const data = JSON.parse(message.text) // 解析 JSON 字符串
					setQrcodeUrl(data.loginUrl) // 从解析后的对象获取 loginUrl
					setCurrentQrcoeSid(data.qrcodeSid) // 从解析后的对象获取 qrcodeSid
					// 立即检查一次状态
					if (data.qrcodeSid) {
						vscode.postMessage({
							type: "getQrcodeStatus",
							text: data.qrcodeSid, // 使用解析后的 qrcodeSid
						} as WebviewMessage)
					}
				} catch (e) {
					console.error("Failed to parse qrcodeInfo data:", e)
				}
			} else if (message.type === "qrcodeStatus") {
				if (typeof message.username === "string" && message.username !== "") {
					// setQrcodeUrl(""); // 更新 state
					// switchTab("chat"); // 使用 switchTab 切换视图
				}
			} else if (message.type === "userInfo") {
				// 如果收到用户信息且已登录，跳转到聊天页面
				if (message.username && message.username !== "") {
					onDone()
				}
			}
		}
		window.addEventListener("message", messageHandler)
		return () => {
			window.removeEventListener("message", messageHandler)
		}
	}, [onDone]) // 依赖项改为 switchTab

	useEffect(() => {
		try {
			// 获取二维码信息
			vscode.postMessage({
				type: "getQrcodeInfo",
			} as WebviewMessage)
		} catch (error) {
			console.log("login error :", error)
		}
	}, []) // 移除 navigate 依赖

	// 定期检查用户是否已登录
	useEffect(() => {
		// 立即检查一次登录状态
		vscode.postMessage({
			type: "getUserInfo",
		} as WebviewMessage)

		// 设置定时器，每5秒检查一次登录状态
		const loginCheckTimer = setInterval(() => {
			vscode.postMessage({
				type: "getUserInfo",
			} as WebviewMessage)
		}, 1000)

		return () => clearInterval(loginCheckTimer)
	}, [])

	useEffect(() => {
		const timer = setInterval(() => {
			setRemainingTime((prev) => {
				if (prev <= 0) {
					// 先检查时间
					clearInterval(timer) // 在回调内部清除计时器
					return 0
				}

				// 仅当 sid 存在且时间 > 0 时获取状态
				if (currentQrcoeSid) {
					vscode.postMessage({
						type: "getQrcodeStatus",
						text: currentQrcoeSid,
					} as WebviewMessage)
				}

				return prev - 1 // 最后减少时间
			})
		}, 1000)

		return () => clearInterval(timer)
	}, [currentQrcoeSid]) // <--- 添加 currentQrcoeSid 到依赖项

	const refreshQRCode = async () => {
		try {
			// 请求新的二维码
			vscode.postMessage({
				type: "getQrcodeInfo",
			} as WebviewMessage)
			// 注意：收到消息后，useEffect 中的 messageHandler 会自动更新 state
			setRemainingTime(qrcodeExpireTime) // 重置倒计时
		} catch (error) {
			console.error("刷新二维码错误:", error)
		}
	}

	return (
		<div className="flex flex-col items-center justify-center min-h-screen">
			<div className="p-8 bg-white rounded-lg shadow-md">
				<h2 className="text-2xl font-bold mb-6 text-center">扫码登录</h2>
				<div className="mb-4">
					{qrcodeUrl && (
						<QRCodeSVG
							value={qrcodeUrl}
							size={200}
							level="M"
							includeMargin={true}
							imageSettings={{
								src:
									"data:image/svg+xml;utf8," +
									encodeURIComponent(`<svg width="35px" height="28px" viewBox="0 0 40 28" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
<defs>
  <linearGradient x1="21.5933397%" y1="31.0156586%" x2="74.6953461%" y2="69.7398622%" id="linearGradient-1">
    <stop stop-color="#83D8FF" offset="0%"></stop>
    <stop stop-color="#3889FF" offset="100%"></stop>
  </linearGradient>
</defs>
<g id="页面-1" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
  <g id="首页-3" transform="translate(-51.000000, -5.000000)">
    <g id="口袋logo" transform="translate(51.000000, 5.000000)">
      <path d="M20.0732,18.2822 C17.5302,18.9652 12.7792,24.7992 12.7792,24.7992 L20.0892,25.2302 C20.0892,25.2302 25.9102,21.6472 26.0772,20.3492 C26.2432,19.0522 22.6172,17.5992 20.0732,18.2822" id="Fill-1" fill="#C8E5FF"></path>
      <path d="M23.9727,17.7226 C23.5597,17.6166 23.0807,17.6606 23.0807,17.6606 C22.9787,17.6676 22.8187,17.7026 22.7217,17.7416 L22.5887,17.7946 C22.4937,17.8326 22.4987,17.8756 22.5997,17.8916 C22.5997,17.8916 23.4637,18.0156 23.7847,18.0986 C24.1047,18.1816 24.7247,18.4356 24.7247,18.4356 C24.8207,18.4736 24.8787,18.4226 24.8567,18.3236 L24.8637,18.3506 C24.8407,18.2506 24.7557,18.1206 24.6707,18.0626 C24.6707,18.0626 24.3857,17.8286 23.9727,17.7226" id="Fill-5" fill="#C8E5FF"></path>
      <path d="M0,16.3078 L0,16.3258 C0.001,17.3498 0.178,18.4098 0.558,19.5088 C2.473,23.9538 7.732,28.2188 17.812,27.6618 C25.502,26.9788 28.706,21.4528 28.706,21.4528 C28.706,21.4528 31.873,17.1588 24.686,16.4318 C23.387,16.3318 22.346,16.2648 21.458,16.4318 C19.322,16.8358 18.204,17.9668 17.492,18.6908 C15.349,20.8678 13.392,23.7198 13.392,23.7198 C13.392,23.7198 17.9,17.7468 21.074,17.2618 C23.27,16.8208 28.376,18.2258 26.852,20.3038 C25.328,22.3818 20.197,26.0428 11.748,23.4108 C3.298,20.7788 11.612,15.2148 17.291,14.6608 C22.971,14.1068 25.78,16.7868 30.257,17.5108 C31.744,17.7328 36.643,18.3558 37.885,13.3598 C37.958,13.0138 37.997,12.6588 38,12.3018 L38,12.2098 C37.983,10.1708 36.821,8.0948 34.567,7.3738 C34.557,7.7108 34.518,8.0468 34.448,8.3758 C34.294,9.1058 33.413,11.2438 31.118,12.1328 C28.731,13.0578 25.105,12.6148 24.728,12.2158 C24.728,12.2158 30.042,13.3738 32.712,10.6258 C33.375,9.9418 33.814,9.0788 34.009,8.1668 C34.073,7.8668 34.109,7.5588 34.116,7.2508 C34.111,7.2488 34.106,7.2468 34.101,7.2458 C34.133,7.2208 34.193,4.9018 32.171,3.7728 C30.471,2.8838 28.707,2.9228 26.989,4.1058 C27.023,4.6238 26.987,5.1438 26.881,5.6458 C26.726,6.3758 25.844,8.5148 23.549,9.4038 C21.163,10.3288 17.538,9.8858 17.161,9.4868 C17.161,9.4868 22.475,10.6448 25.144,7.8958 C25.807,7.2128 26.246,6.3508 26.441,5.4378 C26.675,4.3458 26.545,3.4188 25.97,2.3368 C25.633,1.7518 25.096,1.1598 24.242,0.7028 C23.318,0.2098 22.206,-0.0002 21.011,-0.0002 C18.824,-0.0002 16.357,0.7008 14.243,1.6468 C7.07,4.8748 0.008,9.8318 0,16.3078 Z" id="Fill-9" fill="url(#linearGradient-1)"></path>
    </g>
  </g>
</g>
</svg>
`),
								height: 40,
								width: 40,
								excavate: true,
							}}
						/>
					)}
				</div>
				<button
					onClick={refreshQRCode}
					className="mt-4 p-2 bg-blue-500 text-white rounded w-full"
					style={{ border: "none" }}>
					刷新二维码
				</button>
			</div>
		</div>
	)
}

export default LoginPage
