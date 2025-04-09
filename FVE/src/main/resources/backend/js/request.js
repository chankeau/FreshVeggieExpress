(function (win) {
  // 默认请求头设置，告知服务器请求体格式为 JSON
  // 注意：如果你的应用中有些请求不是 JSON（比如文件上传），可能需要在使用 axios 发送这些特定请求时覆盖 Content-Type
  axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8';

  // 创建axios实例
  const service = axios.create({
    // axios中请求配置有baseURL选项，表示请求URL公共部分
    baseURL: '/', // 基础URL，所有请求会拼接在这个后面
    // 超时时间（单位毫秒），1000000 毫秒 = 1000 秒，可能有点太长了，可以根据需要调整，比如 30000 (30秒)
    timeout: 30000 // 调整为 30 秒
  });

  // request拦截器
  service.interceptors.request.use(config => {
    // --------------------------------------------------
    // ---  核心修改：添加 Accept 请求头 ---
    // --------------------------------------------------
    // 明确告知服务器，客户端期望接收 JSON 格式的响应
    // Accept 的值通常是 'application/json, text/plain, */*'
    // 这表示优先接受 JSON，其次是纯文本，最后是任何其他类型
    if (!config.headers['Accept']) { // 如果请求配置中没有手动设置 Accept 头
      config.headers['Accept'] = 'application/json, text/plain, */*';
    }
    // --------------------------------------------------
    // --- 结束核心修改 ---
    // --------------------------------------------------


    // 是否需要设置 token (这部分逻辑保持不变，根据你的认证需求启用或调整)
    // const isToken = (config.headers || {}).isToken === false
    // const token = getToken(); // 你需要实现 getToken() 方法来获取存储的 token
    // if (token && !isToken) {
    //   config.headers['Authorization'] = 'Bearer ' + token; // Bearer Token 示例
    // }

    // get请求映射params参数 (保持不变)
    if (config.method === 'get' && config.params) {
      let url = config.url + '?';
      for (const propName of Object.keys(config.params)) {
        const value = config.params[propName];
        var part = encodeURIComponent(propName) + "=";
        if (value !== null && typeof(value) !== "undefined") {
          // 处理参数是对象的情况 (例如 { ids: [1, 2] } 转换成 'propName[ids]=1&propName[ids]=2')
          // 注意：这种转换方式不通用，取决于后端如何解析。更常见的可能是 JSON.stringify 或其他序列化方式。
          // 但这里保持你原有的逻辑。
          if (typeof value === 'object' && !(value instanceof Date) && !(value instanceof File) && !(value instanceof Blob)) { // 排除 Date, File, Blob 等特殊对象
            // 对于数组的处理可能需要更精细，例如转换为 propName=1&propName=2 或 propName[]=1&propName[]=2
            // 这里暂时保持你的原始逻辑，它可能适用于特定情况
            Object.keys(value).forEach(key => {
              let params = propName + '[' + key + ']';
              var subPart = encodeURIComponent(params) + "=";
              url += subPart + encodeURIComponent(value[key]) + "&";
            });
          } else {
            // 处理基本类型或 Date/File/Blob 等
            url += part + encodeURIComponent(value) + "&";
          }
        }
      }
      url = url.slice(0, -1); // 去掉最后一个 '&'
      config.params = {}; // 清空 params 对象，因为参数已拼接到 URL
      config.url = url;
    }
    return config;
  }, error => {
    // 请求错误处理
    console.error('Request Error:', error); // 使用 console.error
    // 可以添加全局的错误提示
    if (window.ELEMENT) { // 检查 Element UI 是否可用
      window.ELEMENT.Message({ message: '请求发送失败', type: 'error' });
    }
    return Promise.reject(error); // 将错误继续抛出
  });

  // 响应拦截器
  service.interceptors.response.use(
      /**
       * 如果你想获取 http 信息，例如 headers 或 status
       * 请 return response => response
       */

      /**
       * 通过自定义 code 判断响应状态，这里只是一个例子
       * 你也可以通过 HTTP 状态码来判断状态
       */
      response => {
        // res 是服务器返回的原始响应数据 (通常是 JSON 解析后的对象)
        const res = response.data;

        // code 不为 1，或 code 为 0 且 msg 为 'NOTLOGIN'，判定为错误
        // 你原来的逻辑是只判断 code=0 & msg='NOTLOGIN'
        // 可以根据你的后端 R 类定义调整判断条件
        // 例如，如果 code !== 1 都算错误
        if (res.code !== 1) {
          // 如果是未登录错误 (NOTLOGIN)
          if (res.code === 0 && res.msg === 'NOTLOGIN') {
            console.warn('--- Response Interceptor: NOTLOGIN detected ---');
            // 移除本地存储的用户信息
            localStorage.removeItem('userInfo');
            // 弹出提示
            if (window.ELEMENT) { // 检查 Element UI 是否可用
              window.ELEMENT.MessageBox.confirm(
                  '当前登录状态已失效，请重新登录',
                  '提示',
                  { confirmButtonText: '重新登录', cancelButtonText: '取消', type: 'warning' }
              ).then(() => {
                // 跳转到登录页面
                window.top.location.href = '/backend/page/login/login.html';
              });
            } else {
              alert('登录已失效，请重新登录'); // 备用提示
              window.top.location.href = '/backend/page/login/login.html';
            }
            // 返回一个被拒绝的 Promise，阻止后续的 .then() 执行
            return Promise.reject(new Error(res.msg || 'Not logged in'));
          } else {
            // 其他错误情况 (code 不为 1 但不是 NOTLOGIN)
            console.error('Response Error:', res.msg || 'Unknown error');
            if (window.ELEMENT) {
              window.ELEMENT.Message({ message: res.msg || '操作失败', type: 'error' });
            }
            // 同样返回被拒绝的 Promise
            return Promise.reject(new Error(res.msg || 'Error'));
          }
        } else {
          // code 为 1，表示成功，直接返回 data 部分 (保持不变)
          return res; // 或者 return res.data; 如果你的后端 R 类是 { code: 1, data: {...}, msg: '...' } 结构
        }
      },
      error => {
        // 网络错误或其他请求阶段的错误处理 (保持不变)
        console.error('err' + error); // 使用 console.error
        let { message } = error;
        if (message == "Network Error") {
          message = "后端接口连接异常";
        } else if (message.includes("timeout")) {
          message = "系统接口请求超时";
        } else if (message.includes("Request failed with status code")) {
          // 尝试提取状态码
          const statusCodeMatch = message.match(/status code (\d+)/);
          const statusCode = statusCodeMatch ? statusCodeMatch[1] : '未知';
          message = `系统接口异常 (${statusCode})`; // 更清晰的提示
        }
        if (window.ELEMENT) { // 检查 Element UI
          window.ELEMENT.Message({
            message: message,
            type: 'error',
            duration: 5 * 1000
          });
        } else {
          alert(message); // 备用
        }
        return Promise.reject(error); // 将错误继续抛出
      }
  );

  // 将配置好的 axios 实例挂载到 window 对象上，方便全局调用
  win.$axios = service;
})(window);