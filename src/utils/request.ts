import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const BASE_URL = import.meta.env.VITE_API_PREFIX
export const request = axios.create({
  baseURL: BASE_URL,
  timeout: 600000,
})
request.interceptors.response.use(
  (res) => {
    return res.data
  },
  ({ response }) => {
    console.info('response:', response)

    if (response.data.code === 10012) {
      localStorage.removeItem('X-Token')

      router.push('/')
      return
    }

    if (response.data.code !== 1 && response.data.code !== 401) {
      console.info()
      ElMessage.warning({ message: 'Request failed, please try again later' })
    }

    return Promise.reject(response.data)
  }
)
