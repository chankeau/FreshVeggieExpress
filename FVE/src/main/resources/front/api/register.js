
/**
 * Calls the backend API to register a new user.
 * @param {object} data - User registration data (name, phone, email, sex, idNumber, code)
 * @returns Promise
 */
const register = (data) => {
    return $axios({
        url: '/user/register',
        method: 'post',
        data
    })
}
