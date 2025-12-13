(function() {
  try {
    // 尝试从localStorage获取
    var token = localStorage.getItem('App-Token') ||
                localStorage.getItem('token') ||
                localStorage.getItem('access_token') ||
                localStorage.getItem('Authorization') ||
                localStorage.getItem('userToken');

    // 尝试从sessionStorage获取
    if (!token) {
      token = sessionStorage.getItem('App-Token') ||
              sessionStorage.getItem('Token') ||
              sessionStorage.getItem('access_token');
    }

    // 尝试从Vuex获取
    if (!token && window.$nuxt && window.$nuxt.$store) {
      var user = window.$nuxt.$store.state.user;
      if (user && user.token) token = user.token;
    }

    return token || '';
  } catch(e) {
    console.error('获取token失败:', e);
    return '';
  }
})()